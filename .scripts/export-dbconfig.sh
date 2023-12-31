#!/usr/bin/env bash

if test -f /secrets/dbcreds/username; then
    export DB_USERNAME=$(cat /secrets/dbcreds/username)
    echo "export of username successful"
else
    echo "Fil med brukernavn ikke funnet med path /secrets/dbcreds/username."
fi

if test -f /secrets/dbcreds/password; then
    export DB_PASSWORD=$(cat /secrets/dbcreds/password)
    echo "export of password successful"
else
    echo "Fil med passord ikke funnet med path /secrets/dbcreds/password."
fi

if test -f /secrets/dbconfig/jdbc_url; then
    export DB_JDBC_URL=$(cat /secrets/dbconfig/jdbc_url)
    echo "export of jdbc_url successful"
else
    echo "Fil med JDBC_URL ikke funnet med path secrets/dbconfig/jdbc_url."
fi