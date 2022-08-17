# BPA binary dependencies

## License maven plugin

In the attribution step the build pipeline builds a license container that bundles all 3rd party licenses.
As the maven-license-plugin does not provide all features that are needed we switched to a fork of that plugin
that provides more features, see: https://github.com/JD-CSTx/license-maven-plugin Unfortunately there is 
no version of that plugin available in the official maven repository. As we never ship this dependency and only need 
it in the main pipeline, the dependency was build once locally and then uploaded to the bin folder. 
The maven install plugin will then take it from the bin folder and will install it into the local maven repository so that 
it is available in the attribution build step.