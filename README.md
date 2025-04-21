# CA 생성
    openssl genrsa -out ca.key 2048
    openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt

# 서버 키/CSR/서명
    openssl genrsa -out server.key 2048
    openssl req -new -key server.key -out server.csr
    openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt -days 3650

# 클라이언트 키/CSR/서명
    openssl genrsa -out client.key 2048
    openssl req -new -key client.key -out client.csr
    openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -set_serial 02 -out client.crt -days 3650

# PKCS12 keystore로 변환
    openssl pkcs12 -export -in server.crt -inkey server.key -out server-keystore.p12 -name server
    openssl pkcs12 -export -in client.crt -inkey client.key -out client-keystore.p12 -name client

# truststore 생성
    keytool -import -trustcacerts -alias ca -file ca.crt -keystore server-truststore.p12 -storetype PKCS12
    keytool -import -trustcacerts -alias ca -file ca.crt -keystore client-truststore.p12 -storetype PKCS12

# intermediate 인증서를 truststore에 추가하려면
    keytool -import -alias intermediateCA -file intermediateCA.crt -keystore client-truststore.p12 -storetype PKCS12 -storepass 'nhn!@#123'
    keytool -import -alias intermediateCA -file intermediateCA.crt -keystore server-truststore.p12 -storetype PKCS12 -storepass 'nhn!@#123'

# CSR 생성에 필요한 정보 예시
    countryName = KR
    stateOrProvinceName = Gyeonggi-do
    localityName = Seongnam-si
    organizationName = NHN
    organizationalUnitName = Distribution Platform Development Team
    commonName = localhost
    email = leejeongwha@nhn.com
