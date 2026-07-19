# Signing keystore

The signing keystore is intentionally not committed.

Provide a PKCS#12 keystore through a protected runtime mechanism and set
`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, and `KEYSTORE_ALIAS`. Never commit a
private key or a populated `.p12` file to this repository.
