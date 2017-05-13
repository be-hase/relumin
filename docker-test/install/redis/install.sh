#!/bin/sh

#### Properties ####
APP_PATH=${HOME}/apps
REDIS_DIR=redis-${REDIS_VER}
REDIS_FILENAME=redis-${REDIS_VER}.tar.gz


#### Functions #####
function install_redis
{
    ##### install Redis #####
    echo "Install Redis."

    # clean up
    if [ -L "${APP_PATH}/redis" ]; then
        echo "Remove Redis symbolic link."
        sudo rm -f ${APP_PATH}/redis
    fi
    if [ -d "${APP_PATH}/${REDIS_FILENAME}" ]; then
        echo "Remove installed Redis."
        sudo rm -f ${APP_PATH}/${REDIS_FILENAME}
    fi
    if [ -d "${APP_PATH}/${REDIS_DIR}" ]; then
        echo "Remove installed Redis."
        sudo rm -rf ${APP_PATH}/${REDIS_DIR}
    fi

    #install
    mkdir -p ${APP_PATH}
    cd ${APP_PATH}
    wget http://download.redis.io/releases/${REDIS_FILENAME}
    tar xzf ${APP_PATH}/${REDIS_FILENAME}
    cd ${APP_PATH}/${REDIS_DIR}
    echo "Make Redis."
    if ! make; then
        echo "Failed to install Redis. (make error)"
        exit 1
    fi

    ln -s ${APP_PATH}/${REDIS_DIR} ${APP_PATH}/redis

    echo ">>> Install Redis finished."
}


#### Main ####
install_redis
exit 0
