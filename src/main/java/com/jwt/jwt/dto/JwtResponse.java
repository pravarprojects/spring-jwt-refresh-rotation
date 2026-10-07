package com.jwt.jwt.dto;

public class JwtResponse {
    private String accessToken;
    private String token;

    private JwtResponse() {
    }

    public static JwtResponseBuilder builder() {
        return new JwtResponseBuilder();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getToken() {
        return token;
    }

    public static class JwtResponseBuilder {
        private String accessToken;
        private String token;

        private JwtResponseBuilder() {
        }

        public JwtResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public JwtResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public JwtResponse build() {
            JwtResponse response = new JwtResponse();
            response.accessToken = this.accessToken;
            response.token = this.token;
            return response;
        }
    }
}
