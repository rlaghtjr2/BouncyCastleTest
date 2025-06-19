package com.nhncloud.pca.constant.acme;

import lombok.Getter;

@Getter
public enum ProblemType {
    ACCOUNT_DOES_NOT_EXIST("urn:ietf:params:acme:error:accountDoesNotExist", "The request specified an account that does not exist"),
    ALREADY_REVOKED("urn:ietf:params:acme:error:alreadyRevoked", "The request specified a certificate to be revoked that has already been revoked"),
    BAD_CSR("urn:ietf:params:acme:error:badCSR", "The CSR is unacceptable (e.g., due to a short key)"),
    BAD_NONCE("urn:ietf:params:acme:error:badNonce", "The client sent an unacceptable anti-replay nonce"),
    BAD_PUBLIC_KEY("urn:ietf:params:acme:error:badPublicKey", "The JWS was signed by a public key the server does not support"),
    BAD_REVOCATION_REASON("urn:ietf:params:acme:error:badRevocationReason", "The revocation reason provided is not allowed by the server"),
    BAD_SIGNATURE_ALGORITHM("urn:ietf:params:acme:error:badSignatureAlgorithm", "The JWS was signed with an algorithm the server does not support"),
    CAA("urn:ietf:params:acme:error:caa", "Certification Authority Authorization (CAA) records forbid the CA from issuing a certificate"),
    COMPOUND("urn:ietf:params:acme:error:compound", "Specific error conditions are indicated in the \"subproblems\" array"),
    CONNECTION("urn:ietf:params:acme:error:connection", "The server could not connect to validation target"),
    DNS("urn:ietf:params:acme:error:dns", "There was a problem with a DNS query during identifier validation"),
    EXTERNAL_ACCOUNT_REQUIRED("urn:ietf:params:acme:error:externalAccountRequired", "The request must include a value for the \"externalAccountBinding\" field"),
    INCORRECT_RESPONSE("urn:ietf:params:acme:error:incorrectResponse", "Response received didn't match the challenge's requirements"),
    INVALID_CONTACT("urn:ietf:params:acme:error:invalidContact", "A contact URL for an account was invalid"),
    MALFORMED("urn:ietf:params:acme:error:malformed", "The request message was malformed"),
    ORDER_NOT_READY("urn:ietf:params:acme:error:orderNotReady", "The request attempted to finalize an order that is not ready to be finalized"),
    RATE_LIMITED("urn:ietf:params:acme:error:rateLimited", "The request exceeds a rate limit"),
    REJECTED_IDENTIFIER("urn:ietf:params:acme:error:rejectedIdentifier", "The server will not issue certificates for the identifier"),
    SERVER_INTERNAL("urn:ietf:params:acme:error:serverInternal", "The server experienced an internal error"),
    TLS("urn:ietf:params:acme:error:tls", "The server received a TLS error during validation"),
    UNAUTHORIZED("urn:ietf:params:acme:error:unauthorized", "The client lacks sufficient authorization"),
    UNSUPPORTED_CONTACT("urn:ietf:params:acme:error:unsupportedContact", "A contact URL for an account used an unsupported protocol scheme"),
    UNSUPPORTED_IDENTIFIER("urn:ietf:params:acme:error:unsupportedIdentifier", "An identifier is of an unsupported type"),
    USER_ACTION_REQUIRED("urn:ietf:params:acme:error:userActionRequired", "Visit the 'instance' URL and take actions specified there");

    private final String type;
    private final String description;

    ProblemType(String type, String description) {
        this.type = type;
        this.description = description;
    }
}
