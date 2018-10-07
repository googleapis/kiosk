
ifeq ($(UNAME_S),Linux)
else
  CFLAGS = -Xcc -ISources/BoringSSL/include -Xlinker -lz
endif

all:
	swift build $(CFLAGS) -c release
	cp .build/release/k-swift .
	cp .build/release/kiosk-server-swift .

clean :
	rm -rf Packages googleapis .build
	rm -f Package.pins k-swift Sources/*.pb.swift Sources/*.grpc.swift google.json
	rm -rf Package.resolved
