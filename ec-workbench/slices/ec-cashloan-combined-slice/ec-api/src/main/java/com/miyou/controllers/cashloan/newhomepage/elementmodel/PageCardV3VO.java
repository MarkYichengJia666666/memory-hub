package com.miyou.controllers.cashloan.newhomepage.elementmodel;

import com.miyou.controllers.cashloan.newhomepage.HomePageResponseFields;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementModuleType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Slf4j
public class PageCardV3VO implements HomePageResponseFields {
  private final Map<ElementModuleType, Map<ElementId, IElement>> modelCards = new HashMap<>();

  public PageCardV3VO() {
    for (ElementModuleType type : ElementModuleType.values()) {
      modelCards.put(type, new HashMap<>());
    }
  }

  private Map<ElementId, IElement> getElementMapForModule(ElementModuleType moduleType) {
    return modelCards.get(moduleType);
  }

  private void addElementToElementMap(Map<ElementId, IElement> elementMap, IElement element, boolean replaceIfPresent) {
    if (!replaceIfPresent && elementMap.containsKey(element.getId())) {
      log.warn("Duplicated element when adding element to page card v3 vo, elementId = {} ", element.getId());
    }
    elementMap.put(element.getId(), element);
  }

  public void addElement(ElementModuleType moduleType, IElement element) {
    Map<ElementId, IElement> elementMap = getElementMapForModule(moduleType);
    addElementToElementMap(elementMap, element, false);
  }

  public void addAllElements(ElementModuleType moduleType, List<IElement> elements) {
    Map<ElementId, IElement> elementMap = getElementMapForModule(moduleType);
    elements.forEach(element -> addElementToElementMap(elementMap, element, false));
  }

  public void addAllElements(ElementModuleType moduleType, List<IElement> elements, boolean replaceIfPresent) {
    Map<ElementId, IElement> elementMap = getElementMapForModule(moduleType);
    elements.forEach(element -> addElementToElementMap(elementMap, element, replaceIfPresent));
  }

  public void forceAddElement(ElementModuleType moduleType, IElement element) {
    Map<ElementId, IElement> elementMap = getElementMapForModule(moduleType);
    addElementToElementMap(elementMap, element, true);
  }

  /**
   * 不要使用该方法进行暴力覆盖！！！
   *
   * @param moduleType
   * @param elements
   */
  @Deprecated
  public void forceAddAllElements(ElementModuleType moduleType, List<IElement> elements) {
    addAllElements(moduleType, elements, true);
  }


  // Method for MAIN CARD

  public void addElementForMainCard(IElement element) {
    addElementToElementMap(getElementMapForModule(ElementModuleType.MAIN_CARD), element, false);
  }

  public void forceAddElementForMainCard(IElement element) {
    Map<ElementId, IElement> elementMap = modelCards.get(ElementModuleType.MAIN_CARD);
    addElementToElementMap(elementMap, element, true);
  }

  public void deleteFromElementMap(ElementId elementId) {
    Map<ElementId, IElement> elementMap = modelCards.get(ElementModuleType.MAIN_CARD);
    elementMap.put(elementId, null);
  }

  public Map<ElementId, IElement> getMainCardElementMap() {
    return getElementMapForModule(ElementModuleType.MAIN_CARD);
  }

  public void deleteModule(ElementModuleType moduleType) {
    modelCards.remove(moduleType);
  }
}
