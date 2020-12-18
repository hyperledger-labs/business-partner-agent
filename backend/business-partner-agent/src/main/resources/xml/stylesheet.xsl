<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml">

    <xsl:output method="xml" indent="yes" omit-xml-declaration="no"/>

    <xsl:template match="xhtml:html">
        <attributionReport>
            <dependencies>
                <xsl:for-each select="xhtml:body/descendant::xhtml:table[2]/descendant::xhtml:tr">
                    <xsl:if test="descendant::xhtml:td[2]/xhtml:a">
                        <dependency>
                            <name></name>
                            <groupId>
                                <xsl:value-of select="substring-before(descendant::xhtml:td[2]/xhtml:a, ':')"/>
                            </groupId>
                            <artifactId>
                                <xsl:value-of select="substring-before(substring-after(descendant::xhtml:td[2]/xhtml:a, ':'), ':')"/>
                            </artifactId>
                            <version>
                                <xsl:value-of select="substring-after(substring-after(descendant::xhtml:td[2]/xhtml:a, ':'), ':')"/>
                            </version>
                            <type>
                                <xsl:value-of select="descendant::xhtml:td[5]"/>
                            </type>
                            <licenses>
                                <license>
                                    <name><xsl:value-of select="descendant::xhtml:td[6]"/></name>
                                </license>
                            </licenses>
                        </dependency>
                    </xsl:if>
                </xsl:for-each>
            </dependencies>
        </attributionReport>
    </xsl:template>

</xsl:stylesheet>