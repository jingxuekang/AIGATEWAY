package com.aigateway.provider.adapter;

/**
 * Provider 异常
 */
public class ProviderException extends RuntimeException {
    
    private String providerName;
    private String errorCode;
    
    public ProviderException(String message) {
        super(message);
    }
    
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ProviderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ProviderException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
