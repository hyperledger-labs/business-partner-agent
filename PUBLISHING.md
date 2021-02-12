# Publish docker image and release helm chart

A business partner release consist of a helm chart which references the corresponding business-partner-agent docker image.
The release process is therefore split into two steps: Publish docker image and release helm chart.
(In some cases, we might also want to publish only a new docker image, e.g. for testing purpose.)

## Step 1: Publish docker image

The code to be published should be in the `master` branch.

Create a git tag in the format `v<new version>` (e.g `v1.2.3`)
The incremented version number should adhere to the [Semantic Versioning Specification](https://semver.org/#semantic-versioning-specification-semver) based on the changes since the last published release.
A docker image will be created with the tag `<new version>` (e.g `1.2.3`) by a [github workflow](.github/workflows/build.yml).

## Step 2: Release helm chart

Update the `version` and `appVersion` number listed in [charts/bpa/Chart.yaml](charts/bpa/Chart.yaml).

`appVersion` must be equal to the docker image tag created in the previous step.

`version` is typically the same.

Both incremented version numbers should adhere to the [Semantic Versioning Specification](https://semver.org/#semantic-versioning-specification-semver) based on the changes since the last published release.

A new tag and GitHub release will be created automatically by a [github workflow](.github/workflows/release.yml).
Charts in the charts folder will be automatically deployed to a repository hosted on github pages.