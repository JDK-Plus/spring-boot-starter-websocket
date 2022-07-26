
PROTOC_SRC_DIR=./src/main/proto
PROTOC_OUT_DIR=./src/main/java

build-protoc:
	protoc --plugin=protoc-gen-grpc-java -I=${PROTOC_SRC_DIR} --java_out=${PROTOC_OUT_DIR} ${PROTOC_SRC_DIR}/message.proto

deploy:
	shift password
	mvn deploy -Dgpg.passphrase=${password} -Dmaven.test.skip=true