#!/bin/bash
USER_ID=${LOCAL_USER_ID:-1000}
GROUP_ID=${LOCAL_GROUP_ID:-1000}

echo "Starting with UID : $USER_ID"
usermod -u $USER_ID spring
groupmod -g $GROUP_ID spring

exec gosu spring "$@"
