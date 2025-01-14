/******* XPATH TO NODE code *********/

    // convert an xpath expression to an array of DOM nodes
	function xPathToNodes(xpath) {
	  try {
	    var q = document.evaluate(xpath, document, null, XPathResult.ANY_TYPE,
		                      null);
	    var results = [];

	    var next = q.iterateNext();
	    while (next) {
	      results.push(next);
	      next = q.iterateNext();
	    }
	    return results;
	  } catch (e) {
	    return null;
	  }
	  return [];
	}

	function xPathToNode(xpath) {
	  var nodes = xPathToNodes(xpath);
	  //if we don't successfully find nodes, let's alert
	  if (nodes.length != 1)
	    return null;

	  if (nodes.length >= 1)
	    return nodes[0];
	  else
	    return null;
	}

function nodeToXPath(element) {
	//  we want the full path, not one that uses the id since ids can change
	//  if (element.id !== '')
	//    return 'id("' + element.id + '")';
	  if (element.tagName.toLowerCase() === 'html')
	    return element.tagName;

	  // if there is no parent node then this element has been disconnected
	  // from the root of the DOM tree
	  if (!element.parentNode)
	    return '';

	  var ix = 0;
	  var siblings = element.parentNode.childNodes;
	  for (var i = 0, ii = siblings.length; i < ii; i++) {
	    var sibling = siblings[i];
	    if (sibling === element)
	      return nodeToXPath(element.parentNode) + '/' + element.tagName +
		     '[' + (ix + 1) + ']';
	    if (sibling.nodeType === 1 && sibling.tagName === element.tagName)
	      ix++;
	  }
}

function getFeatures(element){
  var info = {};
  info.xpath = nodeToXPath(element);
  for (var prop in element) {
	  var val = element[prop];
	  if (val !== null && typeof val === 'object'){
	      try{
	        val = val.toString(); //sometimes get that toString not allowed
	      }
	      catch(err){
	        continue;
	      }
	  }
    else if (typeof val === 'function'){
      continue;
    }
	  info[prop] = val;
  } //test

  var text = element.textContent;
  info.textContent = text;
  var trimmedText = text.trim();
  info.firstWord = trimmedText.slice(0,trimmedText.indexOf(" "));
  info.lastWord = trimmedText.slice(trimmedText.lastIndexOf(" "),trimmedText.length);
  var colonIndex = trimmedText.indexOf(":")
  if (colonIndex > -1){
    info.preColonText = trimmedText.slice(0,colonIndex);
  }
  var children = element.childNodes;
  var l = children.length;
  for (var i = 0; i< l; i++){
    var childText = children[i].textContent;
    info["child"+i+"text"] = childText;
    info["lastChild"+(l-i)+"text"] = childText;
  }

  var prev = element.previousElementSibling;
  if (prev !== null){
    info.previousElementSiblingText = prev.textContent;
  }

  var boundingBox = element.getBoundingClientRect();
  for (var prop in boundingBox) {
    if (boundingBox.hasOwnProperty(prop)) {
      info[prop] = boundingBox.prop;
    }
  }
  var style = window.getComputedStyle(element, null);
  for (var i = 0; i < style.length; i++) {
    var prop = style[i];
    info[prop] = style.getPropertyValue(prop);
  }
  return info;
}

var all_features = ['accessKey','accessKeyLabel','align','align-items','align-self','aLink','animation-delay','animation-direction','animation-duration','animation-fill-mode','animation-iteration-count','animation-name','animation-play-state','animation-timing-function','async','ATTRIBUTE_NODE','attributes','autofocus','backface-visibility','background','background-attachment','background-clip','background-color','background-image','background-origin','background-position','background-repeat','background-size','baseURI','bgColor','border-bottom-color','border-bottom-left-radius','border-bottom-right-radius','border-bottom-style','border-bottom-width','border-collapse','border-image-outset','border-image-repeat','border-image-slice','border-image-source','border-image-width','border-left-color','border-left-style','border-left-width','border-right-color','border-right-style','border-right-width','border-spacing','border-top-color','border-top-left-radius','border-top-right-radius','border-top-style','border-top-width','bottom','box-shadow','caption-side','CDATA_SECTION_NODE','charset','child0text','child10text','child11text','child12text','child13text','child14text','child15text','child16text','child17text','child18text','child19text','child1text','child20text','child2text','child3text','child4text','child5text','child6text','child7text','child8text','child9text','childElementCount','childNodes','children','classList','className','clear','clientHeight','clientLeft','clientTop','clientWidth','clip','clip-path','clip-rule','color','color-interpolation','color-interpolation-filters','COMMENT_NODE','compact','content','contentEditable','contextMenu','coords','counter-increment','counter-reset','crossOrigin','cursor','dataset','defer','dir','direction','disabled','display','DOCUMENT_FRAGMENT_NODE','DOCUMENT_NODE','DOCUMENT_POSITION_CONTAINED_BY','DOCUMENT_POSITION_CONTAINS','DOCUMENT_POSITION_DISCONNECTED','DOCUMENT_POSITION_FOLLOWING','DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC','DOCUMENT_POSITION_PRECEDING','DOCUMENT_TYPE_NODE','dominant-baseline','download','draggable','ELEMENT_NODE','empty-cells','ENTITY_NODE','ENTITY_REFERENCE_NODE','event','fill','fill-opacity','fill-rule','filter','firstChild','firstElementChild','firstWord','flex-basis','flex-direction','flex-grow','flex-shrink','float','flood-color','flood-opacity','font-family','font-kerning','font-size','font-size-adjust','font-stretch','font-style','font-synthesis','font-variant','font-variant-alternates','font-variant-caps','font-variant-east-asian','font-variant-ligatures','font-variant-numeric','font-variant-position','font-weight','form','formAction','formEnctype','formMethod','formNoValidate','formTarget','hash','height','hidden','host','hostname','href','hreflang','htmlFor','id','image-rendering','ime-mode','innerHTML','isContentEditable','itemId','itemProp','itemRef','itemScope','itemType','itemValue','justify-content','lang','lastChild','lastChild10text','lastChild11text','lastChild12text','lastChild13text','lastChild14text','lastChild15text','lastChild16text','lastChild17text','lastChild18text','lastChild19text','lastChild1text','lastChild20text','lastChild21text','lastChild2text','lastChild3text','lastChild4text','lastChild5text','lastChild6text','lastChild7text','lastChild8text','lastChild9text','lastElementChild','lastWord','left','letter-spacing','lighting-color','line-height','link','list-style-image','list-style-position','list-style-type','localName','margin-bottom','margin-left','margin-right','margin-top','marker-end','marker-mid','marker-offset','marker-start','mask','mask-type','max-height','max-width','min-height','min-width','-moz-appearance','-moz-background-inline-policy','-moz-binding','-moz-border-bottom-colors','-moz-border-left-colors','-moz-border-right-colors','-moz-border-top-colors','-moz-box-align','-moz-box-direction','-moz-box-flex','-moz-box-ordinal-group','-moz-box-orient','-moz-box-pack','-moz-box-sizing','-moz-column-count','-moz-column-fill','-moz-column-gap','-moz-column-rule-color','-moz-column-rule-style','-moz-column-rule-width','-moz-column-width','-moz-float-edge','-moz-font-feature-settings','-moz-font-language-override','-moz-force-broken-image-icon','-moz-hyphens','-moz-image-region','-moz-orient','-moz-osx-font-smoothing','-moz-outline-radius-bottomleft','-moz-outline-radius-bottomright','-moz-outline-radius-topleft','-moz-outline-radius-topright','-moz-stack-sizing','-moz-tab-size','-moz-text-align-last','-moz-text-blink','-moz-text-decoration-color','-moz-text-decoration-line','-moz-text-decoration-style','-moz-text-size-adjust','-moz-user-focus','-moz-user-input','-moz-user-modify','-moz-user-select','-moz-window-shadow','name','namespaceURI','nextElementSibling','nextSibling','nodeName','nodeType','nodeValue','NOTATION_NODE','offsetHeight','offsetLeft','offsetParent','offsetTop','offsetWidth','onabort','onafterprint','onbeforeprint','onbeforeunload','onblur','oncanplay','oncanplaythrough','onchange','onclick','oncontextmenu','oncopy','oncut','ondblclick','ondrag','ondragend','ondragenter','ondragleave','ondragover','ondragstart','ondrop','ondurationchange','onemptied','onended','onerror','onfocus','onhashchange','oninput','oninvalid','onkeydown','onkeypress','onkeyup','onload','onloadeddata','onloadedmetadata','onloadstart','onmessage','onmousedown','onmouseenter','onmouseleave','onmousemove','onmouseout','onmouseover','onmouseup','onmozfullscreenchange','onmozfullscreenerror','onmozpointerlockchange','onmozpointerlockerror','onoffline','ononline','onpagehide','onpageshow','onpaste','onpause','onplay','onplaying','onpopstate','onprogress','onratechange','onreset','onresize','onscroll','onseeked','onseeking','onselect','onshow','onstalled','onsubmit','onsuspend','ontimeupdate','onunload','onvolumechange','onwaiting','onwheel','opacity','order','outerHTML','outline-color','outline-offset','outline-style','outline-width','overflow','overflow-x','overflow-y','ownerDocument','padding-bottom','padding-left','padding-right','padding-top','page-break-after','page-break-before','page-break-inside','paint-order','parentElement','parentNode','pathname','perspective','perspective-origin','ping','pointer-events','port','position','preColonText','prefix','previousElementSibling','previousElementSiblingText','previousSibling','PROCESSING_INSTRUCTION_NODE','properties','protocol','quotes','rel','resize','rev','right','scrollHeight','scrollLeft','scrollLeftMax','scrollTop','scrollTopMax','scrollWidth','search','shape','shape-rendering','spellcheck','src','stop-color','stop-opacity','stroke','stroke-dasharray','stroke-dashoffset','stroke-linecap','stroke-linejoin','stroke-miterlimit','stroke-opacity','stroke-width','style','tabIndex','table-layout','tagName','target','text','text-align','text-anchor','textContent','text-decoration','text-indent','TEXT_NODE','text-overflow','text-rendering','text-shadow','text-transform','title','top','transform','transform-origin','transform-style','transition-delay','transition-duration','transition-property','transition-timing-function','type','unicode-bidi','validationMessage','validity','value','vector-effect','vertical-align','visibility','vLink','white-space','width','willValidate','word-break','word-spacing','word-wrap','writing-mode','xpath','z-index']

var func_a1 = function(url,xpath,url1,url2){
	var node = xPathToNode(xpath);
	var features = getFeatures(node);
	
	var feature_string = "";
	var skip = false;
	for(var i = 0; i< all_features.length; i++){
		var value = features[all_features[i]];
		if (value && typeof value === "string" && (value.indexOf("<,>") > -1 || value.indexOf("@#@") > -1)){
			skip = true; //no row for this one
			break;
		}
		feature_string+="<,>"+value;
	}

	if (!skip){
	    return url+"<,>"+xpath+"<,>"+url1+"<,>"+url2+feature_string;
    }
    return;
};
