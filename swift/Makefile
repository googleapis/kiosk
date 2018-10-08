
ifeq ($(UNAME_S),Linux)
else
  CFLAGS = -Xlinker -lz
endif

all:
	./COMPILE-PROTOS.sh
	swift build $(CFLAGS) 
	cp .build/debug/k-swift .
	cp .build/debug/kiosk-server-swift .

clean :
	rm -rf Packages .build
	rm -f Package.pins k-swift kiosk-server-swift
	rm -rf Sources/*/generated Sources/generated
	rm -rf Package.resolved kiosk_descriptor.pb
