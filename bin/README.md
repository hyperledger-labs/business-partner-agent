# BPA binary dependencies

## License maven plugin

In the attribution build pipeline a license container is build that bundles all 3rd party licenses.
As the maven-license-plugin does not provide all features that are needed we switched to a fork of that plugin
that provides more features, see: https://github.com/JD-CSTx/license-maven-plugin Unfortunately there is 
no version of that plugin available in the official maven repository. As we never ship this dependency and only need 
it in the attribution pipeline, the dependency is added as a submodule and referenced there.