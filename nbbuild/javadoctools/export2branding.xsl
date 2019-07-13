<?xml version="1.0" encoding="UTF-8" ?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:param name="date" />

    <xsl:template match="/" >
        <html>
        <head>
            <!-- projects.netbeans.org -->
           <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
           <title>Description Layer Registration in Apache NetBeans (incubating) APIs</title>
            <link rel="stylesheet" href="netbeans.css" type="text/css"/>

          <link REL="icon" href="http://www.netbeans.org/favicon.ico" type="image/ico" />
          <link REL="shortcut icon" href="http://www.netbeans.org/favicon.ico" />

        </head>

        <body>
            <center><h1>Description of Branding APIs for Apache NetBeans (incubating) Platform Applications</h1></center>

            <p>
            Applications built on top of NetBeans Platform may change configuration
            of various aspects via branding. E.g. by providing different values
            for certain keys in various <code>Bundle</code>. 
            This page summarizes the list of such
            branding APIs defined by <a href="index.html">modules with API</a>. 
            </p>

            <ul>
            <xsl:for-each select="//api[@type='export' and @group='branding']" >
                <li>
                    <b>
                        <xsl:choose >
                            <xsl:when test="@url" >
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="@url"/>
                                    </xsl:attribute>
                                    <xsl:value-of select="@name"></xsl:value-of>
                                </a>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@name"></xsl:value-of>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:text> in </xsl:text>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="ancestor::module/@target"/>
                                <xsl:text>#group-branding</xsl:text>
                            </xsl:attribute>
                            <xsl:value-of select="ancestor::module/@name"/>
                        </a>
                    </b>
                    <p>
                    <xsl:apply-templates select="." />
                    </p>
                </li>
            </xsl:for-each>
            </ul>
            
            <p>
                To get your API listed here, use 
                <code>&lt;api type='export' group='branding' ... /&gt;</code> in
                your module arch.xml document.
            </p>
         </body>
         </html>
    </xsl:template>
    
    <xsl:template match="api-ref">
        <!-- simply bold the name, it link will likely be visible bellow -->
        <b>
            <xsl:value-of select="@name" />
        </b>
    </xsl:template>

    <xsl:template match="usecase">
        <h4><xsl:value-of select="@name" /></h4>
        <xsl:apply-templates select="./node()" />
    </xsl:template>

    <xsl:template match="a[@href]">
        <xsl:variable name="target" select="ancestor::module/@target"/>
        <xsl:variable name="top" select="substring-before($target,'/')" />
        
          <xsl:call-template name="print-url" >
            <xsl:with-param name="url" select="@href" />
            <xsl:with-param name="base" select="$target" />
            <xsl:with-param name="top" select="$top" />
          </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="print-url" >
        <xsl:param name="url" />
        <xsl:param name="base" />
        <xsl:param name="top" />
        
        <xsl:choose>
            <xsl:when  test="contains(@href,'@TOP@')" >
                <xsl:comment>URL contains @TOP@</xsl:comment>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$top" />
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="substring-after($url,'@TOP@')" />
                    </xsl:attribute>
                    <xsl:apply-templates />
                </a>
            </xsl:when>
            <xsl:when test="contains($url,'//')" >
                <xsl:comment>This is very likely URL with protocol, if not see nbbuild/javadoctools/export2usecases.xsl</xsl:comment>
                <a> 
                    <xsl:attribute name="href">
                        <xsl:value-of select="$url" />
                    </xsl:attribute>
                    <xsl:apply-templates />
                </a>
            </xsl:when>
            <xsl:when test="starts-with($url, '#')" >
                <xsl:comment>Probably reference in the same target document</xsl:comment>
                <a href="{$base}{$url}" >
                    <xsl:apply-templates />
                </a>
            </xsl:when>
            <xsl:when test="starts-with($url, 'org-')" >
                <xsl:comment>Probably reference relative to root</xsl:comment>
                <a href="{$url}" >
                    <xsl:apply-templates />
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:comment>This must be a reference relative to the arch page, if not see nbbuild/javadoctools/export2usecases.xsl
            </xsl:comment>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$base" />
                        <xsl:text>/../</xsl:text>
                        <xsl:value-of select="$url" />
                    </xsl:attribute>
                    <xsl:apply-templates />
                </a>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
            
    <xsl:template match="@*|node()">
       <xsl:copy  >
          <xsl:apply-templates select="@*|node()"/>
       </xsl:copy>
    </xsl:template>
        
</xsl:stylesheet>


