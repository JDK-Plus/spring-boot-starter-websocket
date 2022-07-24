
PROTOC_SRC_DIR=./src/main/protoc
PROTOC_OUT_DIR=./src/main/java

build-protoc:
	protoc -I=${PROTOC_SRC_DIR} --java_out=${PROTOC_OUT_DIR} ${PROTOC_SRC_DIR}/message.proto

deploy:
	shift password
	mvn deploy -Dgpg.passphrase=${password} -Dmaven.test.skip=true