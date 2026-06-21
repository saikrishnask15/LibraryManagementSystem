FROM ubuntu:latest
LABEL authors="saikr"

ENTRYPOINT ["top", "-b"]