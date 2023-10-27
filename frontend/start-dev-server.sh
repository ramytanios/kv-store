#!/bin/sh
nix run github:ramytanios/fs2-live-server#native --refresh -- --entry-file=index.html --browser=firefox
