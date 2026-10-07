package com.jwt.jwt.entity;


import jakarta.persistence.*;
import java.time.Instant;

@Entity(name="refresh_rotation")
public class RefreshToken {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String token;

    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_entity_id")
    private UserEntity userEntity;
    
    private Instant generationTime;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean revoked = false;

    public RefreshToken(String token, Instant expiryDate, UserEntity userEntity,
			Instant generationTime) {
		super();
		this.token = token;
		this.expiryDate = expiryDate;
		this.userEntity = userEntity;

		this.generationTime = generationTime;
	}

	
    	public RefreshToken() {
    	super();
    	}

    public static class Builder {
        private int id;
        private String token;
        private Instant expiryDate;
        private UserEntity userEntity;
        
        private Instant generationTime;
        private boolean revoked = false;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder expiryDate(Instant expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder userEntity(UserEntity userEntity) {
            this.userEntity = userEntity;
            return this;
        }
        
        public Builder generationTime(Instant generationTime) {
        	this.generationTime=generationTime;
        	return this;
        }

        public Builder revoked(boolean revoked) {
        	this.revoked = revoked;
        	return this;
        }

        public RefreshToken build() {
            return new RefreshToken(this);
        }

		
    }

    private RefreshToken(Builder builder) {
        this.id = builder.id;
        this.token = builder.token;
        this.expiryDate = builder.expiryDate;
        this.userEntity = builder.userEntity;

        this.generationTime=builder.generationTime;
        this.revoked=builder.revoked;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter methods for fields

    public int getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }
   
	public void setToken(String token) {
		this.token = token;
	}

	public void setExpiryDate(Instant expiryDate) {
		this.expiryDate = expiryDate;
	}

	public void setUserEntity(UserEntity userEntity) {
		this.userEntity = userEntity;
	}

	public Instant getGenerationTime() {
		return generationTime;
	}

	public void setGenerationTime(Instant generationTime) {
		this.generationTime = generationTime;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

}