
ifeq ($(UNAME_S),Linux)
else
  CFLAGS = -Xlinker -lz
endif

all:
	./COMPILE-PROTOS.sh
	swift build $(CFLAGS) 
	cp .build/debug/kiosk-tool-swift .
	cp .build/debug/kiosk-server-swift .

clean :
	rm -rf .build Packages Package.pins Package.resolved
	rm -rf Sources/*/generated Sources/generated
	rm -rf kiosk_descriptor.pb
	rm -rf kiosk-tool-swift kiosk-server-swift
