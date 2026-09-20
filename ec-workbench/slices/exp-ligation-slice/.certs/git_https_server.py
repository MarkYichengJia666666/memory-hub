#!/usr/bin/env python3
"""Smart HTTPS git-http-backend for local CodeGraph clone (self-signed)."""
import os
import ssl
import subprocess
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CERTDIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(ROOT)
PORT = int(os.environ.get("GIT_HTTPS_PORT", "8443"))


class GitHTTP(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        sys.stderr.write("git-http " + (fmt % args) + "\n")

    def do_GET(self):
        self._cgi()

    def do_POST(self):
        self._cgi()

    def _cgi(self):
        parsed = urlparse(self.path)
        env = os.environ.copy()
        env["GIT_PROJECT_ROOT"] = PROJECT_ROOT
        env["GIT_HTTP_EXPORT_ALL"] = "1"
        env["PATH_INFO"] = parsed.path
        env["QUERY_STRING"] = parsed.query or ""
        env["REQUEST_METHOD"] = self.command
        env["CONTENT_TYPE"] = self.headers.get("Content-Type", "")
        length = int(self.headers.get("Content-Length", "0") or 0)
        env["CONTENT_LENGTH"] = str(length)
        env["REMOTE_USER"] = "git"
        env["REMOTE_ADDR"] = self.client_address[0]
        body = self.rfile.read(length) if length else b""
        proc = subprocess.run(
            ["git", "http-backend"],
            input=body,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=env,
        )
        out = proc.stdout
        if proc.returncode != 0 and not out:
            self.send_error(500, proc.stderr.decode("utf-8", "replace")[:300])
            return
        sep = out.find(b"\r\n\r\n")
        if sep >= 0:
            header_blob, payload = out[:sep], out[sep + 4 :]
            lines = header_blob.split(b"\r\n")
        else:
            sep = out.find(b"\n\n")
            header_blob, payload = (out[:sep], out[sep + 2 :]) if sep >= 0 else (b"", out)
            lines = header_blob.split(b"\n")
        status = 200
        headers = []
        for line in lines:
            if not line.strip():
                continue
            if line.lower().startswith(b"status:"):
                try:
                    status = int(line.split(b":", 1)[1].strip().split()[0])
                except Exception:
                    status = 200
                continue
            if b":" in line:
                k, v = line.split(b":", 1)
                headers.append((k.decode(), v.strip().decode()))
        self.send_response(status)
        for k, v in headers:
            self.send_header(k, v)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


def main():
    httpd = HTTPServer(("0.0.0.0", PORT), GitHTTP)
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(os.path.join(CERTDIR, "cert.pem"), os.path.join(CERTDIR, "key.pem"))
    httpd.socket = ctx.wrap_socket(httpd.socket, server_side=True)
    print("smart_https_ready port=%s root=%s" % (PORT, PROJECT_ROOT), flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
