// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.docgen;

import com.google.common.collect.Sets;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Assembles the multi-page version of the Build Encyclopedia with one page per rule family.
 */
public class MultiPageBuildEncyclopediaProcessor extends BuildEncyclopediaProcessor {
  public MultiPageBuildEncyclopediaProcessor(
      RuleLinkExpander linkExpander, ConfiguredRuleClassProvider ruleClassProvider) {
    super(linkExpander, ruleClassProvider);
  }

  /**
   * Collects and processes all the rule and attribute documentation in inputDirs and generates the
   * Build Encyclopedia into the outputDir.
   *
   * @param inputDirs list of directory to scan for document in the source code
   * @param outputDir output directory where to write the build encyclopedia
   * @param denyList optional path to a file listing rules to not document
   */
  @Override
  public void generateDocumentation(List<String> inputDirs, String outputDir, String denyList)
      throws BuildEncyclopediaDocException, IOException {
    BuildDocCollector collector = new BuildDocCollector(linkExpander, ruleClassProvider, false);
    Map<String, RuleDocumentation> ruleDocEntries = collector.collect(inputDirs, denyList);
    warnAboutUndocumentedRules(
        Sets.difference(ruleClassProvider.getRuleClassMap().keySet(), ruleDocEntries.keySet()));

    writeStaticDoc(outputDir, "make-variables");
    writeStaticDoc(outputDir, "functions");
    writeCommonDefinitionsPage(outputDir);

    writeRuleDocs(outputDir, ruleDocEntries.values());
  }

  private void writeStaticDoc(String outputDir, String name) throws IOException {
    // TODO(dzc): Consider splitting out the call to writePage so that this method only creates the
    // Page object and adding docgen tests that test the state of Page objects constructed by
    // this method, and similar methods in this class.
    Page page = TemplateEngine.newPage(DocgenConsts.BE_TEMPLATE_DIR + "/" + name + ".vm");
    page.add("expander", linkExpander);
    writePage(page, outputDir, name + ".html");
  }

  private void writeCommonDefinitionsPage(String outputDir) throws IOException {
    Page page = TemplateEngine.newPage(DocgenConsts.COMMON_DEFINITIONS_TEMPLATE);
    page.add("expander", linkExpander);
    page.add("typicalAttributes", expandCommonAttributes(PredefinedAttributes.TYPICAL_ATTRIBUTES));
    page.add("commonAttributes", expandCommonAttributes(PredefinedAttributes.COMMON_ATTRIBUTES));
    page.add("testAttributes", expandCommonAttributes(PredefinedAttributes.TEST_ATTRIBUTES));
    page.add("binaryAttributes", expandCommonAttributes(PredefinedAttributes.BINARY_ATTRIBUTES));
    writePage(page, outputDir, "common-definitions.html");
  }

  private void writeRuleDocs(String outputDir, Iterable<RuleDocumentation> docEntries)
      throws BuildEncyclopediaDocException, IOException {
    RuleFamilies ruleFamilies = assembleRuleFamilies(docEntries);

    // Generate documentation.
    writeOverviewPage(outputDir, ruleFamilies.langSpecific, ruleFamilies.generic);
    writeBeNav(outputDir, ruleFamilies.all);
    for (RuleFamily ruleFamily : ruleFamilies.all) {
      if (ruleFamily.size() > 0) {
        writeRuleDoc(outputDir, ruleFamily);
      }
    }
  }

  private void writeOverviewPage(String outputDir,
      List<RuleFamily> langSpecificRuleFamilies,
      List<RuleFamily> genericRuleFamilies)
      throws BuildEncyclopediaDocException, IOException {
    Page page = TemplateEngine.newPage(DocgenConsts.OVERVIEW_TEMPLATE);
    page.add("expander", linkExpander);
    page.add("langSpecificRuleFamilies", langSpecificRuleFamilies);
    page.add("genericRuleFamilies", genericRuleFamilies);
    writePage(page, outputDir, "overview.html");
  }

  private void writeRuleDoc(String outputDir, RuleFamily ruleFamily)
      throws BuildEncyclopediaDocException, IOException {
    Page page = TemplateEngine.newPage(DocgenConsts.RULES_TEMPLATE);
    page.add("ruleFamily", ruleFamily);
    page.add("expander", linkExpander);
    writePage(page, outputDir, ruleFamily.getId() + ".html");
  }

  private void writeBeNav(String outputDir, List<RuleFamily> ruleFamilies) throws IOException {
    Page page = TemplateEngine.newPage(DocgenConsts.BE_NAV_TEMPLATE);
    page.add("ruleFamilies", ruleFamilies);
    writePage(page, outputDir, "be-nav.html");
  }
}
