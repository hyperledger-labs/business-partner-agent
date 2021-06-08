# Publish docker image and release helm chart

A business partner release consist of a [helm chart](https://github.com/hyperledger-labs/business-partner-agent-chart) which references the corresponding business-partner-agent docker image.
The release process is therefore split into two steps: Publish docker image and release helm chart.
(In some cases, we might also want to publish only a new docker image, e.g. for testing purpose. Same for the chart.)

## Versioning 

In general we use [Semantic Versioning 2.0.0](https://semver.org/lang/de/).
For releases we stick to `0` for MAJOR as long a we are somehow in "alpha" and increment the MINOR for each release.
Helm [chart version and docker image version (== appVersion)](https://github.com/hyperledger-labs/business-partner-agent-chart/blob/main/charts/bpa/Chart.yaml) should be the same when releasing.

For pre-releases we use a pre-release suffix for the chart version and the sha for the docker image.

Example release history:

| Chart version | Docker image version | Comment |
| -------- | -------- | -------- |
| 0.2-alpha01   | sha-86b02ee6   | new features being tested   |
| 0.2-alpha02   | sha-...   | new features being tested    |
| 0.2-rc1   | sha-...   | feature freeze for 0.2   |
| 0.2.0   | 0.2.0   | 0.2 release   |
| 0.3-alpha01   | sha-...  | start development of 0.3   |
| ...   | ...   | ...   |

## Publish docker image

The code to be published should be in the `master` branch.
A docker image will be created and pushed to our github registry automatically by a [github workflow](.github/workflows/build.yml) - tagged with the sha.

For defined versions (e.g. `0.2.0`), create a git tag in the format `<new version>` to trigger the workflow.

## Release helm chart

Create a PR in our [helm repository](https://github.com/hyperledger-labs/business-partner-agent-chart) with your changes.
At least `version` and `appVersion` have to be increased in [Chart.yaml](https://github.com/hyperledger-labs/business-partner-agent-chart/blob/main/charts/bpa/Chart.yaml). 
