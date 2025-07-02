-- ACME 테이블 샘플 데이터 INSERT 쿼리

-- 1. ACME 계정 생성
INSERT INTO PCA_ACME_ACCOUNT (
    status,
    contact,
    terms_of_service_agreed,
    external_account_binding,
    creation_user,
    last_change_user
) VALUES (
    'VALID',
    '["mailto:admin@example.com", "mailto:webmaster@example.com"]',
    TRUE,
    NULL,
    'system',
    'system'
);

-- 2. ACME 주문 생성
INSERT INTO PCA_ACME_ORDER (
    account_id,
    status,
    expires,
    not_before,
    not_after,
    finalize_url,
    certificate_url,
    creation_user,
    last_change_user
) VALUES (
    1,
    'PENDING',
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 90 DAY),
    'https://ca.example.com/acme/finalize/12345',
    NULL,
    'system',
    'system'
);

-- 3. 식별자 생성 (도메인 정보)
INSERT INTO PCA_ACME_IDENTIFIER (
    order_id,
    type,
    value
) VALUES
(1, 'dns', 'example.com'),
(1, 'dns', 'www.example.com');

-- 4. 인증 생성 (각 식별자별로)
INSERT INTO PCA_ACME_AUTHORIZATION (
    order_id,
    identifier_id,
    status,
    expires,
    wildcard,
    creation_user,
    last_change_user
) VALUES
(1, 1, 'PENDING', DATE_ADD(NOW(), INTERVAL 7 DAY), FALSE, 'system', 'system'),
(1, 2, 'PENDING', DATE_ADD(NOW(), INTERVAL 7 DAY), FALSE, 'system', 'system');

-- 5. 챌린지 생성 (각 인증별로)
INSERT INTO PCA_ACME_CHALLENGE (
    authorization_id,
    type,
    status,
    url,
    token,
    validated,
    error
) VALUES
-- HTTP-01 챌린지
(1, 'HTTP_01', 'PENDING', 'https://ca.example.com/acme/challenge/abc123', 'abc123', NULL, NULL),
-- DNS-01 챌린지 (백업용)
(1, 'DNS_01', 'PENDING', 'https://ca.example.com/acme/challenge/def456', 'def456', NULL, NULL),
-- HTTP-01 챌린지 (두 번째 도메인)
(2, 'HTTP_01', 'PENDING', 'https://ca.example.com/acme/challenge/ghi789', 'ghi789', NULL, NULL),
-- DNS-01 챌린지 (두 번째 도메인 백업용)
(2, 'DNS_01', 'PENDING', 'https://ca.example.com/acme/challenge/jkl012', 'jkl012', NULL, NULL);

-- 6. 챌린지 검증 완료 예시
UPDATE PCA_ACME_CHALLENGE
SET status = 'VALID',
    validated = NOW(),
    last_change_datetime = NOW()
WHERE id = 1;

-- 7. 인증 완료 예시
UPDATE PCA_ACME_AUTHORIZATION
SET status = 'VALID',
    last_change_datetime = NOW()
WHERE id = 1;

-- 8. 주문 상태 업데이트 (모든 인증이 완료된 경우)
UPDATE PCA_ACME_ORDER
SET status = 'READY',
    last_change_datetime = NOW()
WHERE id = 1;

-- 9. 인증서 발급 후 주문 상태 업데이트
UPDATE PCA_ACME_ORDER
SET status = 'VALID',
    certificate_url = 'https://ca.example.com/acme/cert/67890',
    last_change_datetime = NOW()
WHERE id = 1;

-- 추가 샘플 데이터

-- 와일드카드 인증서 예시
INSERT INTO PCA_ACME_ACCOUNT (status, contact, terms_of_service_agreed, creation_user, last_change_user)
VALUES ('VALID', '["mailto:admin@wildcard.com"]', TRUE, 'system', 'system');

INSERT INTO PCA_ACME_ORDER (account_id, status, expires, creation_user, last_change_user)
VALUES (2, 'PENDING', DATE_ADD(NOW(), INTERVAL 7 DAY), 'system', 'system');

INSERT INTO PCA_ACME_IDENTIFIER (order_id, type, value)
VALUES (2, 'dns', '*.wildcard.com');

INSERT INTO PCA_ACME_AUTHORIZATION (order_id, identifier_id, status, expires, wildcard, creation_user, last_change_user)
VALUES (2, 3, 'PENDING', DATE_ADD(NOW(), INTERVAL 7 DAY), TRUE, 'system', 'system');

INSERT INTO PCA_ACME_CHALLENGE (authorization_id, type, status, url, token)
VALUES (3, 'DNS_01', 'PENDING', 'https://ca.example.com/acme/challenge/wildcard123', 'wildcard123');

-- 실패한 챌린지 예시 (에러 정보 포함)
INSERT INTO PCA_ACME_CHALLENGE (authorization_id, type, status, url, token, error)
VALUES (3, 'HTTP_01', 'INVALID', 'https://ca.example.com/acme/challenge/failed123', 'failed123',
        '{"type": "urn:ietf:params:acme:error:connection", "detail": "Connection refused", "status": 400}');

-- 만료된 주문 예시
INSERT INTO PCA_ACME_ORDER (account_id, status, expires, creation_user, last_change_user)
VALUES (1, 'EXPIRED', DATE_SUB(NOW(), INTERVAL 1 DAY), 'system', 'system');

-- External Account Binding이 있는 계정 예시
INSERT INTO PCA_ACME_ACCOUNT (
    status,
    contact,
    terms_of_service_agreed,
    external_account_binding,
    creation_user,
    last_change_user
) VALUES (
    'VALID',
    '["mailto:admin@eab.com"]',
    TRUE,
    '{"kid": "key-identifier-123", "hmac": "hmac-value-456", "alg": "HS256"}',
    'system',
    'system'
);