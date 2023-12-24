mkdir ssl
echo "${KEY}" >> ssl/private_key.pem
echo "${CERTS}" >> ssl/certs.pem
echo "${PWD}"