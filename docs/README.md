# Documentation

## Overview

The Business Parter Agent documentation is hosted at [https://hyperledger-labs.github.io/business-partner-agent](https://hyperledger-labs.github.io/business-partner-agent). 

Below is information on how to contribute to the documentation.

## Technology

We have chosen to use [Sphinx](https://www.sphinx-doc.org/en/master/) for our documentation framework, with a few plugins to make life easier using [Markdown](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet) (instead of reStructuredText) and importing diagrams from [Diagrams.net](https://www.diagrams.net) (formerly draw.io).  We use GitHub Actions to build the documentation and get it ready for GitHub Pages to automatically pick it up and host it.   


### Generate Documentation

simply run the following in the root `/` folder:

```shell
make docs
```

The generated static html will be built in the `build/html` folder (as described below).

### View Documentation

After generating the documentation, simply run the following in the root `/` folder:

```shell
docker-compose --profile docs up -d
```

then navigate to [http://localhost:5000](http://localhost:5000) - your changes will automatically be mounted so simply call `make docs` after you make your changes and refresh the browser to see your changes. 

### Diagrams.net (Draw.io) support

[Diagrams.net](https://www.diagrams.net) (formally draw.io) is a free online diagram software for making flowcharts, process diagrams, org charts, UML, ER and network diagrams. 

To reference a file, simply add in the following snippet to your documentation markdown:

~~~
```{drawio-figure} my-awesome-model.drawio
```
~~~

This allows you to simply save your file and not have to export a new image file for each small change (removing the possibility of out-of-sync diagram and image).  

### File/Folder Structure

```
build
│
└───html
    ├─  ...
    └─  index.html


source
│   ├─ index.rst    
│   └─ conf.py
│
├───section
│   ├─  example.rst
│   └─  example.drawio
│   
└───section
    ├─  example.rst
    └─  example.drawio
```

### `/build/**`

Contains generated documentation is .gitignore(d)

### `/build/html/**`

Contains generated static html that can be deployed as any other static html site - for example to: [Read the Docs](https://readthedocs.org/), [GitHub Pages](https://pages.github.com/), etc

### `/source/**`

Contains the markdown documentation. We highly recommend separating each section & subsection into its own folder so that you can include any diagrams alongside the .rst file.

### `/source/index.rst`

This is your landing page and should contain the tree structure of your documentation.  

**Note**: if you add a new chapter or section, you will need to manually reference that section in the  ```.. toctree::``` of the `index.rst` so the user can navigate to it.  

### `/source/conf.py`

This is the project settings for the [Sphinx](https://www.sphinx-doc.org/en/master/) builder

