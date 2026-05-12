#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Elite Vault — Linux Installer
#
# No root or sudo required.
# Installs to ~/.local/share/elite-vault/ (XDG_DATA_HOME)
# Registers a systemctl user service that starts on login / boot.
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/OWNER/elite-vault/main/distribution/installer.sh | bash
#   — or —
#   bash installer.sh          (if you already have the zip extracted)
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
GITHUB_REPO="OWNER/elite-vault"          # ← fill in before release
GITHUB_API="https://api.github.com/repos/${GITHUB_REPO}/releases/latest"

INSTALL_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/elite-vault"
SERVICE_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user"
SERVICE_NAME="elite-vault"
JAR_NAME="elite-vault.jar"
JAVA_MIN=21
DEFAULT_PORT=8085

# ── Colours ───────────────────────────────────────────────────────────────────
if [ -t 1 ]; then
    RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; BOLD=''; NC=''
fi

info()  { echo -e "${GREEN}[elite-vault]${NC} $*"; }
warn()  { echo -e "${YELLOW}[elite-vault]${NC} $*"; }
error() { echo -e "${RED}[elite-vault]${NC} $*" >&2; }
die()   { error "$*"; exit 1; }

# ── Java check ────────────────────────────────────────────────────────────────
check_java() {
    command -v java &>/dev/null || die "Java not found. Install Java ${JAVA_MIN}+ first.
    Ubuntu/Debian:  sudo apt install openjdk-21-jre
    Fedora/RHEL:    sudo dnf install java-21-openjdk
    Or from:        https://adoptium.net"

    local ver
    ver=$(java -version 2>&1 | head -1 | sed 's/.*version "\([0-9]*\).*/\1/')
    [ "${ver}" -ge "${JAVA_MIN}" ] 2>/dev/null || \
        die "Java ${JAVA_MIN}+ required (found Java ${ver}). Please upgrade."

    info "Java ${ver} — OK"
}

# ── Download ──────────────────────────────────────────────────────────────────
download_jar() {
    local tmp_zip
    tmp_zip="$(mktemp /tmp/elite-vault-XXXXXX.zip)"
    trap 'rm -f "${tmp_zip}"' RETURN

    info "Fetching latest release from github.com/${GITHUB_REPO}..."

    # Prefer curl, fall back to wget
    local fetch_url
    if command -v curl &>/dev/null; then
        fetch_url=$(curl -fsSL "${GITHUB_API}" \
            | grep '"browser_download_url"' \
            | grep '\.zip"' \
            | head -1 \
            | cut -d '"' -f 4)
        [ -n "${fetch_url}" ] || die "No zip found in latest release. Check https://github.com/${GITHUB_REPO}/releases"
        info "Downloading ${fetch_url}..."
        curl -fL -o "${tmp_zip}" "${fetch_url}"
    elif command -v wget &>/dev/null; then
        fetch_url=$(wget -qO- "${GITHUB_API}" \
            | grep '"browser_download_url"' \
            | grep '\.zip"' \
            | head -1 \
            | cut -d '"' -f 4)
        [ -n "${fetch_url}" ] || die "No zip found in latest release."
        info "Downloading ${fetch_url}..."
        wget -q --show-progress -O "${tmp_zip}" "${fetch_url}"
    else
        die "Neither curl nor wget found. Install one and retry."
    fi

    info "Extracting to ${INSTALL_DIR}..."
    mkdir -p "${INSTALL_DIR}"
    unzip -o "${tmp_zip}" "${JAR_NAME}" -d "${INSTALL_DIR}"
    info "JAR installed at ${INSTALL_DIR}/${JAR_NAME}"
}

# ── Systemd user service ──────────────────────────────────────────────────────
install_service() {
    # Detect java binary path (needed for ExecStart — PATH may not be set in services)
    local java_bin
    java_bin=$(command -v java)

    mkdir -p "${SERVICE_DIR}"

    cat > "${SERVICE_DIR}/${SERVICE_NAME}.service" << EOF
[Unit]
Description=Elite Vault — EDDN ingest and trade API
Documentation=https://github.com/${GITHUB_REPO}
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
ExecStart=${java_bin} -jar ${INSTALL_DIR}/${JAR_NAME}
Restart=on-failure
RestartSec=15

# Override port if needed:
# Environment=ELITE_VAULT_PORT=${DEFAULT_PORT}

[Install]
WantedBy=default.target
EOF

    systemctl --user daemon-reload
    systemctl --user enable "${SERVICE_NAME}.service"
    systemctl --user start  "${SERVICE_NAME}.service"

    info "Service installed and started."
}

# ── Linger (run without active login session) ─────────────────────────────────
enable_linger() {
    # loginctl enable-linger lets systemd user services survive logout.
    # Useful on headless / server setups. Skipped if already enabled.
    if loginctl show-user "$(id -un)" 2>/dev/null | grep -q 'Linger=yes'; then
        return
    fi
    # This does NOT require root — it just sets a flag for the current user.
    if loginctl enable-linger 2>/dev/null; then
        info "Linger enabled — service will run at boot without needing an active login."
    else
        warn "Could not enable linger. Service will only run while you are logged in."
        warn "To fix: loginctl enable-linger $(id -un)"
    fi
}

# ── Uninstall helper ──────────────────────────────────────────────────────────
uninstall() {
    info "Stopping and removing Elite Vault..."
    systemctl --user stop    "${SERVICE_NAME}.service" 2>/dev/null || true
    systemctl --user disable "${SERVICE_NAME}.service" 2>/dev/null || true
    rm -f "${SERVICE_DIR}/${SERVICE_NAME}.service"
    systemctl --user daemon-reload
    warn "JAR and data left in ${INSTALL_DIR} — remove manually if desired."
    info "Uninstalled."
    exit 0
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
    if [[ "${1:-}" == "--uninstall" ]]; then uninstall; fi

    echo ""
    echo -e "${BOLD}  Elite Vault Installer${NC}"
    echo    "  ══════════════════════"
    echo ""

    check_java
    download_jar
    install_service
    enable_linger

    echo ""
    info "Installation complete!"
    echo ""
    echo -e "  API:      ${BOLD}http://localhost:${DEFAULT_PORT}${NC}"
    echo -e "  Swagger:  ${BOLD}http://localhost:${DEFAULT_PORT}/swagger${NC}"
    echo ""
    echo    "  Useful commands:"
    echo    "    systemctl --user status  elite-vault"
    echo    "    systemctl --user stop    elite-vault"
    echo    "    systemctl --user start   elite-vault"
    echo    "    journalctl --user -u elite-vault -f"
    echo    "    bash installer.sh --uninstall"
    echo ""
}

main "$@"
