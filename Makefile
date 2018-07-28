
all:
	cd server; go install .
	cd k; go install .

clean:
	rm -rf protos/api-common-protos
	rm -rf generated

