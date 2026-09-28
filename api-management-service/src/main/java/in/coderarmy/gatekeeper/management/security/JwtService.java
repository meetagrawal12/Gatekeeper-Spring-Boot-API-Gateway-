package in.coderarmy.gatekeeper.management.security;

import in.coderarmy.gatekeeper.management.entity.ManagementUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Issues signed access tokens. Verification is done by the resource-server JwtDecoder. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration ttl;

    public JwtService(JwtEncoder encoder,
                      @Value("${gatekeeper.jwt.ttl-minutes:60}") long ttlMinutes) {
        this.encoder = encoder;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(ManagementUser user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("gatekeeper-management")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().getName()));
        if (user.getOrganizationId() != null) {
            claims.claim("org", user.getOrganizationId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
