.PHONY: all
.PHONY: docs

all: docs

docs:
	docker run --rm -v `pwd`/docs:/docs jaredweinfurtner/sphinx-drawio-docker make html