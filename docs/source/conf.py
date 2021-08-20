# -- Project information -----------------------------------------------------

project = 'Business Partner Agent'

# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
    'sphinxcontrib.drawio',
    'sphinx_rtd_theme',
    'myst_parser',
]

# fix for the following error:
# master file /home/jared/Projects/TOP90/energy-eot/docs/source/contents.rst not found
# Makefile:20: recipe for target 'html' failed
# make: *** [html] Error 2
master_doc = 'index'

# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
html_theme = 'sphinx_rtd_theme'

# If true, "(C) Copyright ..." is shown in the HTML footer. Default is True.
html_show_copyright = False

# -- Options for draw.io plugin ----------------------------------------------
# @see https://pypi.org/project/sphinxcontrib-drawio/

drawio_default_transparency = True
drawio_headless = True
drawio_no_sandbox = True
drawio_binary_path = '/usr/bin/drawio'

# -- Options for latexpdf ----------------------------------------------------
# By default, Sphinx documentation outputs a PDF that's formatted for duplex printing, so there are alternating blank
# pages in the generated pdf.  The following options disables this:

latex_elements = {
    'extraclassoptions': 'openany,oneside'
}
