package com.furimeo.gitkoo.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.furimeo.gitkoo.auth.UserService;

/**
 * Serves a user's avatar.
 *
 * <p>If the account has an avatar URL stored, the request is redirected there.
 * Otherwise an identicon is generated from the username: a 5x5 grid, mirrored down
 * the middle, coloured from a hash of the name. Same idea as GitHub's default
 * avatar, and the same reason for it - every account gets something recognisable
 * without anyone uploading a file.
 *
 * <p>Generated rather than fetched from Gravatar on purpose. A self-hosted forge
 * should not tell a third party which of its users someone is looking at, and
 * should keep working with no outbound network at all.
 */
@Controller
public class AvatarController {

    private final UserService userService;

    public AvatarController(UserService userService) {
        this.userService = userService;
    }

    /** Foreground colours, picked to stay legible on both light and dark canvases. */
    private static final String[] PALETTE = {
        "#0969da", "#1f883d", "#8250df", "#bf3989", "#bc4c00",
        "#9a6700", "#0550ae", "#1a7f37", "#6639ba", "#a40e26"
    };

    @GetMapping("/avatars/{username}")
    @ResponseBody
    public ResponseEntity<String> avatar(@PathVariable String username) {
        String stored = userService.findByUsername(username)
                .map(u -> u.getAvatar())
                .filter(a -> a != null && !a.isBlank())
                .orElse(null);
        if (stored != null) {
            return ResponseEntity.status(302).header("Location", stored).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                // Deterministic output, so it can be cached hard.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(identicon(username));
    }

    /**
     * Builds a symmetric identicon for {@code seed}.
     *
     * <p>Columns 0-1 are drawn from the hash and mirrored into columns 3-4, with
     * column 2 down the centre. That mirroring is what makes the result read as a
     * deliberate mark rather than visual noise.
     */
    String identicon(String seed) {
        byte[] hash = sha256(seed);
        String colour = PALETTE[Math.floorMod(hash[hash.length - 1], PALETTE.length)];

        StringBuilder cells = new StringBuilder();
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 5; row++) {
                // One hash byte per cell; the low bit decides whether it is filled.
                if ((hash[col * 5 + row] & 1) == 0) {
                    continue;
                }
                cells.append(rect(col, row));
                if (col < 2) {
                    cells.append(rect(4 - col, row));
                }
            }
        }

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 5 5\" "
                + "shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"Avatar for "
                + escape(seed) + "\">"
                + "<rect width=\"5\" height=\"5\" fill=\"#eaeef2\"/>"
                + "<g fill=\"" + colour + "\">" + cells + "</g>"
                + "</svg>";
    }

    private String rect(int x, int y) {
        return "<rect x=\"" + x + "\" y=\"" + y + "\" width=\"1\" height=\"1\"/>";
    }

    private byte[] sha256(String seed) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every JVM; this cannot happen.
            throw new IllegalStateException(e);
        }
    }

    /** The username reaches an attribute, so the five XML metacharacters must go. */
    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
