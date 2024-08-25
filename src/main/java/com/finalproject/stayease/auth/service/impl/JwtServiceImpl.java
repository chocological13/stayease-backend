package com.finalproject.stayease.auth.service.impl;

import com.finalproject.stayease.auth.repository.AuthRedisRepository;
import com.finalproject.stayease.auth.service.JwtService;
import com.finalproject.stayease.users.entity.User;
import com.finalproject.stayease.users.entity.User.UserType;
import com.finalproject.stayease.users.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final AuthRedisRepository authRedisRepository;
  private final UserService userService;

  public JwtServiceImpl(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, AuthRedisRepository authRedisRepository, UserService userService) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.authRedisRepository = authRedisRepository;
    this.userService = userService;
  }

  @Override
  public String generateAccessToken(Authentication authentication) {
    Instant now = Instant.now();

    List<String> authorities = authentication.getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    User user = userService.findByEmail(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException(
        "User not found"));

    JwtClaimsSet claimsSet = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(now)
        .expiresAt(now.plus(1, ChronoUnit.HOURS))
        .subject(authentication.getName())
        .claim("userId", user.getId())
        .claim("userType", user.getUserType())
        .claim("authorities", authorities)
        .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
  }

  @Override
  public String generateRefreshToken(Authentication authentication) {
    Instant now = Instant.now();

    User user = userService.findByEmail(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException(
        "User not found"));

    JwtClaimsSet claimsSet = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(now)
        .expiresAt(now.plus(7, ChronoUnit.DAYS))
        .subject(authentication.getName())
        .claim("userId", user.getId())
        .build();

    String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();

    // Store the refresh token in Redis
    authRedisRepository.saveJwtKey(user.getEmail(), refreshToken);

    return refreshToken;
  }

  @Override
  public String getToken(String email) {
    return authRedisRepository.getJwtKey(email);
  }

  @Override
  public String extractUsername(String token) {
    return extractClaim(token, Jwt::getSubject);
  }

  public <T> T extractClaim(String token, Function<Jwt, T> claimsResolver) {
    final Jwt jwt = decodeToken(token);
    return claimsResolver.apply(jwt);
  }

  @Override
  public void invalidateToken(String email) {
    authRedisRepository.blacklistKey(email);
  }

  @Override
  public boolean isRefreshTokenValid(String token, String email) {
    return authRedisRepository.isValid(token, email);
  }

  @Override
  public boolean isAccessTokenValid(String accessToken, String email) {
    try {
      Jwt jwt = decodeToken(accessToken);
      String tokenEmail = jwt.getSubject();
      return (tokenEmail != null && tokenEmail.equals(email) && !isTokenExpired(jwt));
    } catch (JwtException e) {
      // Token is invalid or expired
      return false;
    }
  }

  private boolean isTokenExpired(Jwt jwt) {
    return jwt.getExpiresAt().isBefore(Instant.now());
  }

  @Override
  public Jwt decodeToken(String token) {
    try {
      return jwtDecoder.decode(token);
    } catch (JwtException e) {
      // Handle invalid token
      throw new RuntimeException("Invalid JWT token: " + e.getLocalizedMessage());
    }
  }

  @Override
  public Authentication getAuthenticationFromToken(String token) {
    Jwt jwt = decodeToken(token);

    Collection<GrantedAuthority> authorities = jwt.getClaimAsStringList("authorities")
        .stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    User principal = new User();
    principal.setId(jwt.getClaim("userId"));
    principal.setEmail(jwt.getSubject());
    principal.setUserType(UserType.valueOf(jwt.getClaim("userType")));

    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }
}
