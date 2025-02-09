.PHONY: build tag push all

IMAGE_NAME=thisguygil/bobo
TAG=latest

build:
	docker build -t bobo:$(TAG) .

tag: build
	docker tag bobo:$(TAG) $(IMAGE_NAME):$(TAG)

push: tag
	docker push $(IMAGE_NAME):$(TAG)

all: build tag push
