mkdir ssl
mkdir .aws
echo "${KEY}" >> ssl/private_key.pem
echo "${CERTS}" >> ssl/certs.pem
echo "${AWS_CREDENTIALS}" >> .aws/credentials
echo "${PWD}"