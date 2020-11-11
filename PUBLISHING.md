# How to Publish a New Version

0. The code to be published should be in the `master` branch.

1. Update the version number listed in [charts/bpa/Chart.yaml](charts/bpa/Chart.yaml). The incremented version number should adhere to the [Semantic Versioning Specification](https://semver.org/#semantic-versioning-specification-semver) based on the changes since the last published release.

3. A new tag and GitHub release will be created automatically by a github workflow (.github/workflows/release.yml).
Charts in the charts folder will be automatically deployed to a repository hosted on github pages.

*TODO* Include the additions to CHANGELOG.md in the release notes.