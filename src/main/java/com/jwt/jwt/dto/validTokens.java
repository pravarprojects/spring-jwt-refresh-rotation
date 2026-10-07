package com.jwt.jwt.dto;

import java.util.ArrayList;

public class validTokens {
    public int userId;
    public ArrayList<String> tokens;

    public validTokens(int userId, ArrayList<String> tokens) {
        this.userId = userId;
        this.tokens = tokens;
    }
}
