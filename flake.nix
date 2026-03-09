{
  description = "sopra-fs26-template-server";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-24.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachSystem ["aarch64-darwin" "x86_64-darwin" "x86_64-linux" "aarch64-linux"] (
      system: let
        overlays = [
          (self: super: {
            jdk = super.jdk17;
          })
        ];

        inherit (nixpkgs) lib;
        pkgs = import nixpkgs {
          inherit system;
          overlays = overlays;
        };

        nativeBuildInputs = with pkgs;
          [
            jdk
            git
          ]
          ++ lib.optionals stdenv.isDarwin [
            xcodes
          ]
          ++ lib.optionals (system == "aarch64-linux") [
            qemu
          ];
      in {
        devShells.default = pkgs.mkShell {
          inherit nativeBuildInputs;

          shellHook = ''
            export HOST_PROJECT_PATH="$(pwd)"
            export COMPOSE_PROJECT_NAME=sopra-fs26-template-server
            
            export PATH="${pkgs.jdk}/bin:$PATH"
            export PATH="${pkgs.git}/bin:$PATH"
            
            XCODE_VERSION_OLD="15.3"
            XCODE_VERSION="16.2"
            XCODE_BUILD_OLD="15E204a" 
            XCODE_BUILD="16C5032a"
            if [[ $(uname) == "Darwin" ]] && [ -z "$CI" ]; then
              if ! (xcodes installed | grep "$XCODE_VERSION ($XCODE_BUILD)" -q || xcodes installed | grep "$XCODE_VERSION_OLD ($XCODE_BUILD_OLD)" -q); then
                echo -e "\e[1;33m================================================\e[0m"
                echo -e "\e[1;33mIf you wish to code in XCode, please install $XCODE_VERSION or $XCODE_VERSION_OLD\e[0m"
                echo -e "\e[1;33mYou can install the latest version with \e[0m\e[1;32mxcodes install $XCODE_VERSION\e[0m\e[1;33m\e[0m"
                echo -e "\e[1;33m================================================\e[0m"
              fi
            fi

            if [[ $(uname) == "Darwin" ]]; then
              echo "export NODE_BINARY=\"$(which node)\"" > .xcode.env.local
            fi

            export PATH=$(echo $PATH | tr ':' '\n' | grep -v clang | paste -sd ':' -)
          '';
        };
      }
    );
}
