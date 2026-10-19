package com.example.demo.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    public JWTAuthorizationFilter(
            org.springframework.security.authentication.AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader(
                SecurityConstants.HEADER_STRING
        );

        if (header == null ||
                !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {

            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                getAuthentication(request);

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(
            HttpServletRequest request) {

        String token = request.getHeader(
                SecurityConstants.HEADER_STRING
        );

        if (token != null) {

            String username = JWT.require(
                            Algorithm.HMAC512(SecurityConstants.SECRET)
                    )
                    .build()
                    .verify(
                            token.replace(
                                    SecurityConstants.TOKEN_PREFIX,
                                    ""
                            )
                    )
                    .getSubject();

            if (username != null) {
                return new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        new ArrayList<>()
                );
            }
        }

        return null;
    }
}