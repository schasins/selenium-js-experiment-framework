var getTarget;
var getTargetFunction;
var targetFunctions;
var getTargetId;
var getTargetClass;
var xPathToNode;

(function() {

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
	    //getLog('misc').error('xPath throws error when evaluated', xpath);
	  }
	  return [];
	}

	function simpleXPathToNode(xpath) {
	  // error was thrown, attempt to just walk down the dom tree
	  var currentNode = document.documentElement;
	  var paths = xpath.split('/');
	  // assume first path is "HTML"
	  paths: for (var i = 1, ii = paths.length; i < ii; ++i) {
	    var children = currentNode.children;
	    var path = paths[i];
	    var splits = path.split(/\[|\]/);

	    var tag = splits[0];
	    if (splits.length > 1) {
	      var index = parseInt(splits[1]);
	    } else {
	      var index = 1;
	    }

	    var seen = 0;
	    children: for (var j = 0, jj = children.length; j < jj; ++j) {
	      var c = children[j];
	      if (c.tagName == tag) {
		seen++;
		if (seen == index) {
		  currentNode = c;
		  continue paths;
		}
	      }
	    }
	    throw 'Cannot find child';
	  }
	  return [currentNode];
	}

	xPathToNode = function(xpath) {
	  var nodes = xPathToNodes(xpath);
	  if (nodes == null){
	  	return;
	  }
	  //if we don't successfully find nodes, let's alert
	  if (nodes.length != 1){
	    //getLog('misc').error("xpath doesn't return strictly one node", xpath);
	    }

	  if (nodes.length >= 1)
	    return nodes[0];
	  else
	    return null;
	}

  function getTargetSimple(targetInfo) {
    return xPathToNodes(targetInfo.xpath);
  }

  function getTargetSuffix(targetInfo) {

    function helper(xpath) {
      var index = 0;
      while (xpath[index] == '/')
        index++;

      if (index > 0)
        xpath = xpath.slice(index)

      var targets = xPathToNodes('//' + xpath);
   
      if (targets.length > 0) {
        return targets;
      }

      // If we're here, we failed to find the child. Try dropping
      // steadily larger prefixes of the xpath until some portion works.
      // Gives up if only three levels left in xpath.
      if (xpath.split("/").length < 4){
        // No more prefixes to reasonably remove, so give up
        return [];
      }

      var index = xpath.indexOf("/");
      xpathSuffix = xpath.slice(index+1);
      return helper(xpathSuffix);
    }

    return helper(targetInfo.xpath);
  }

  function getTargetText(targetInfo) {
    var text = targetInfo.snapshot.prop.innerText;
    if (text) {
      return xPathToNodes('//*[text()="' + text + '"]');
    }
    return [];
  }

  function getTargetSearch(targetInfo) {
    // search over changes to the ancesters (replacing each ancestor with a
    // star plus changes such as adding or removing ancestors)

    function helper(xpathSplit, index) {
      if (index == 0)
        return [];

      var targets;

      if (index < xpathSplit.length - 1) {
        var clone = xpathSplit.slice(0);
        var xpathPart = clone[index];

        clone[index] = '*';
        targets = xPathToNodes(clone.join('/'));
        if (targets.length > 0)
          return targets;

        clone.splice(index, 0, xpathPart);
        targets = xPathToNodes(clone.join('/'));
        if (targets.length > 0)
          return targets;
      } 

      targets = xPathToNodes(xpathSplit.join('/'));
      if (targets.length > 0)
        return targets;

      return helper(xpathSplit, index - 1);
    }

    var split = targetInfo.xpath.split('/');
    return helper(split, split.length - 1);
  }

  getTargetClass = function(targetInfo) {
    var className = targetInfo.snapshot.prop.className;
    if (className) {
      //xPathToNodes("//*[@class='" + className + "']");

      var classes = className.trim().replace(':', '\\:').split(' ');
      var selector = "";
      for (var i = 0, ii = classes.length; i < ii; ++i) {
        var className = classes[i];
        if (className)
          selector += '.' + classes[i];
      }

      return document.querySelectorAll(selector);
    }
    return [];
  }

  getTargetId = function(targetInfo) {
    var id = targetInfo.snapshot.prop.id;
    if (id) {
      var selector = "#" + id.trim().replace(':', '\\:');
      return document.querySelectorAll(selector);
    }
    return [];
  }

  function getTargetComposite(targetInfo) {
    var targets = [];
    var metaInfo = [];

    for (var strategy in targetFunctions) {
      var strategyTargets = targetFunctions[strategy](targetInfo);
      for (var i = 0, ii = strategyTargets.length; i < ii; ++i) {
        var t = strategyTargets[i];
        var targetIndex = targets.indexOf(t);
        if (targetIndex == -1) {
          targets.push(t);
          metaInfo.push([strategy]);
        } else {
          metaInfo[targetIndex].push(strategy);
        }
      }
    }

    var maxStrategies = 0;
    var maxTargets = [];
    for (var i = 0, ii = targets.length; i < ii; ++i) {
      var numStrategies = metaInfo[i].length;
      if (numStrategies == maxStrategies) {
        maxTargets.push(targets[i]);
      } else if (numStrategies > maxStrategies) {
        maxTargets = [targets[i]];
        maxStrategies = numStrategies;
      }
    }

    return maxTargets;
  }

  getTargetFunction = getTargetComposite;

  getTarget = function(targetInfo) {
    var targets = getTargetFunction(targetInfo);
    if (!targets) {
      log.debug('No target found');
      return null
    } else if (targets.length > 1) {
      log.debug('Multiple targets found:', targets);
      return targets[0];
    } else {
      return targets[0];
    }
  };

  targetFunctions = {
    simple: getTargetSimple,
    suffix: getTargetSuffix,
    text: getTargetText,
    class: getTargetClass,
    id: getTargetId,
    search: getTargetSearch
  }

})()

function getAllCandidates(){
  return document.getElementsByTagName("*");
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
	  if (element.hasOwnProperty(prop)) {
	    info[prop] = element.prop;
	  }
	}
	var prev = element.previousElementSibling;
	if (prev){
		info.previousElementSiblingText = prev.innerText;
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


var bestScore = -1;
var secondBestScore = -1;

  getTargetForSimilarity = function(targetInfo) {
    var candidates = getAllCandidates();
    var bestNode = null;
    bestScore = -1;
    secondBestScore = -1;
    for (var i = 0; i<candidates.length; i++){
		var info = getFeatures(candidates[i]);
		var similarityCount = 0;
		for (var prop in targetInfo) {
		  if (targetInfo.hasOwnProperty(prop)) {
		    if (targetInfo[prop] === info[prop]){
	              similarityCount += 1;
		    }
		  }
		}
		if (similarityCount > bestScore){
		  secondBestScore = bestScore;
		  bestScore = similarityCount;
		  bestNode = candidates[i];
		}
    }
    return bestNode;
  };

//iMacros
var retrieveTargetForIMacros = function(targetInfo){
    var ls = document.querySelectorAll(targetInfo.nodeName);
    var textMatchCount = 0;
    for (var i = 0; i<ls.length; i++){
        var node = ls[i];
        if (node.textContent == targetInfo.textContent){
           textMatchCount ++;
           if (textMatchCount == targetInfo.pos){
              return node;
           }
        }
    }
    return;
}


//ATA-QV

var saveTargetInfoForATAQV = function(target){
	//console.log("saveTargetInfoForATAQV");
    var label = getLabel(target);
    //console.log("label", label);

    var nodes = getnodesWithLabelInSubtree(label,$("html"));
    if (nodes.length === 1){
    	//console.log("just one node with this label!");
    	return JSON.stringify({"l":label,"a":[]});
    }
    //console.log(nodes.length, "nodes with this label");
    
    //must find anchors

    
    var t_i = subtreeThatLacksOtherInstancesOfNodeLabel(target);
    var t_others = [];
    for (var i = 0; i <nodes.length; i++){
    	if(nodes[i] === target){
    		//console.log("Good, we found the original node using our label.");
    	}
    	t_others.push(subtreeThatHasNodeLacksNode(nodes[i],target));
    }
    
    var anchors = [];
    while (t_others.length > 0){
    	var distinguishing_label = getDistinguishingLabel(t_i,t_others);
    	if (distinguishing_label !== null){
    		anchors.push(distinguishing_label);
    		return JSON.stringify({"l":label,"a":anchors});
    	}
    	
    	//didn't find anchor.  must continue
    	var dict = findClosestSubtrees(t_i, t_others); //multiple subtrees
    	var t_cl = dict.subtrees;
    	distinguishing_label = getDistinguishingLabel(t_i,t_cl);
    	if (distinguishing_label === null){
    		//give up
    		//console.log("weren't able to find a label that distinguishes t_i from t_cl");
    		//console.log("t_i", t_i);
    		//console.log("t_cl", t_cl);
    		return JSON.stringify({"l":label,"a":null});
    	}
    	//console.log("found a distinguishing label for t_cl", distinguishing_label);
    	//console.log("t_i", t_i);
    	//console.log("t_cl", t_cl);
    	anchors.push(distinguishing_label);
    	
    	t_i = dict.parent;
    	var new_t_others = [];
    	for (var j = 0; j<t_others.length; j++){
    		var include = true;
	    	for (var i = 0; i<t_cl.length; i++){
	    		if (t_others[j].is(t_cl[i])){
	    			include = false;
	    		}
	    	}
	    	if (include){
	    		new_t_others.push(t_others[j]);
	    	}
	    }
	    t_others = new_t_others;
	    //console.log("new_t_others", new_t_others);
    	
    }
    
    //console.log("t_others empty and still no luck");
    return JSON.stringify({"l":label,"a":null});
};

var findClosestSubtrees = function(t_i, t_others){
	//console.log("findClosestSubtrees");
	var currentNode = t_i;
	while(true){
		var parent = currentNode.parent();
		var descendants = parent.find("*");
		descendants.push(parent);
		var subtrees = [];
		for (var i = 0; i<descendants.length; i++){
			var d = descendants[i];
			for (var j = 0; j<t_others.length; j++){
				if (t_others[j].is(d)){
					subtrees.push(d);
				}
			}
		}
		if (subtrees.length > 0){
			return {"parent":parent,"subtrees":subtrees};
		}
		currentNode = parent;
	}
};

var getDistinguishingLabel = function(t_i,t_others){
	//console.log(getDistinguishingLabel);
	var nodes = $.makeArray(t_i.find("*"));
	nodes.push(t_i.get(0));
	var candidates = [];
	for (var i = 0; i<nodes.length; i++){
		////console.log("nodes[i]", nodes[i]);
		candidates.push(getLabel(nodes[i]));
	}
	
	labels_to_avoid = [];
	//console.log("t_others", t_others);
	for (var i = 0; i<t_others.length; i++){
		////console.log(t_others[i]);
		var bad_nodes = $.makeArray($(t_others[i]).find("*"));
		bad_nodes.push(t_others[i]);
		////console.log("bad_nodes", bad_nodes);
		for (var j = 0; j<bad_nodes.length; j++){
			////console.log("bad_nodes[j]", bad_nodes[j]);
			labels_to_avoid.push(getLabel(bad_nodes[j]));
		}
	}
	
	filtered_candidates = [];
	for (var i = 0; i<candidates.length; i++){
		if (labels_to_avoid.indexOf(candidates[i]) === -1){
			filtered_candidates.push(candidates[i]);
		}
	}
	if (filtered_candidates.length === 0){
		return null;
	}
	for (var i = 0; i<filtered_candidates.length; i++){
		var c = filtered_candidates[i];
		if (c.indexOf("textContent") > -1){ //prefer ones that are text content instead of node name
			return c;
		}
	}
	return filtered_candidates[0];
};


var subtreeThatHasNodeLacksNode = function(node1,node2){
	//console.log("subtreeThatHasNodeLacksNode");
    var currentSubtree = $(node1);
    var $node2 = $(node2)
    counter = 0;
	while(counter < 50 && true){
		counter ++;
		var parent = currentSubtree.parent();
		//console.log("parent in subtreeThatHasNodeLacksNode", parent);
		var descendants = parent.find("*");
		for (var i = 0; i< descendants.length ; i++){
			if ($node2.is(descendants[i])){
				//console.log("Good, we found the other label.");
				return currentSubtree;
			}
		}
		currentSubtree  = parent;
	}
};

var subtreeThatLacksOtherInstancesOfNodeLabel = function(node){
	var $body = $("body");
	//console.log("subtreeThatLacksOtherInstancesOfNodeLabel");
	var l = getLabel(node);
	var currentSubtree = $(node);
	counter = 0;
	while(true){
		counter ++;
		var parent = currentSubtree.parent();
		if (parent.is($body)){
			return parent;
		}
		//console.log("parent in subtreeThatLacksOtherInstancesOfNodeLabel", parent);
		var descendants = parent.find("*");
		var l_count = 0;
		for (var i = 0; i<descendants.length; i++){
			if (getLabel(descendants[i])===l){
				l_count += 1;
				if (l_count > 1){
					return currentSubtree;
				}
			}
		}
		currentSubtree = parent;
	}
};

var getLabel = function(node){
    var label = "";
    label = node.textContent;
    if (label === "" || label === undefined){
    	label = node.nodeName+"*****nodeName";
    }
    else{
    	label = label+"*****textContent";
    }
    return label;
};

var getnodesWithLabelInSubtree = function(label,root){
	//console.log("getnodesWithLabelInSubtree");
	var ls = [];
	var arr = label.split("*****");
	var finder = arr[0];
	var tp = arr[1];
	//console.log("finder", finder);
	if (tp === "nodeName"){
		//console.log("using nodeName");
		ls = root.find(finder);
		//console.log(ls);
	}
	else{
		//console.log("using textContent");
		var nodes = root.find("*");
        nodes.push(root); //also include the root itself
    	for (var i = 0; i< nodes.length; i++){
    		if (nodes[i].textContent === finder){
    			ls.push(nodes[i]);
    		}
    	}
	}
	//console.log("ls", ls);
	return ls;
};

var getTargetForATAQV = function(targetInfo){
	//console.log("getTargetForATAQV");
	//console.log(targetInfo);
	var label = targetInfo.l;
	var anchors = targetInfo.a;
	//console.log("anchors", anchors);
	var currentNode = $("html");
	while(true){
	
		//first let's short circuit this if the current node has 0 or 1 possible options for us
		var candidates = getnodesWithLabelInSubtree(label,currentNode);
		if (candidates.length === 0){
			return null;
		}
		if (candidates.length === 1 || anchors === null){
			return candidates[0];
		}
	
		var children = currentNode.children();
		var foundChild = false;
		for (var i = 0; i<children.length; i++){
			var child = children[i];
			//console.log(child);
			var descendants = $(child).find("*");
			descendants.push(child);
			var labels = [];
			for (var j = 0; j<descendants.length; j++){
				var l1 = getLabel(descendants[j]);
				labels.push(l1);
			}

			if (labels.indexOf(label) === -1){
				continue;
			}
			var useThisChild = true;
			for (var j = 0; j< anchors.length; j++){
				//console.log("anchor", anchors[j], labels.indexOf(anchors[j]));
				if (labels.indexOf(anchors[j]) === -1){
					//this subtree doesn't have all anchors
					//console.log("Couldn't find this anchor, better move to the next child");
					useThisChild = false;
					break;
				}
			}
			if(!useThisChild){
				continue;
			}
			//if we've made it here, the subtree has all anchors, go down till no one child has all of them
			currentNode = $(child);
			//console.log("found a child with all", child);
			foundChild = true;
			break;
		}
		//at this point either we found a child with all of them or we know there is no such child
		if(!foundChild){
			//no child with all, time to drop an anchor and try again
			if (anchors.length === 0){
				//well, no more than we can drop, and we don't know a winner
				if (candidates.length > 0){
					return candidates[0]; //just guess
				}
				return null;
			}
			//console.log("couldn't find a child with all, throw out anchor", anchors[anchors.length-1]);
			anchors = anchors.slice(0,anchors.length-1);
		}
	}
};


function simulateClick(node) {
    var evt = document.createEvent("MouseEvents");
    evt.initMouseEvent("click", true, true, window, 1, 0, 0, 0, 0,
        false, false, false, false, 0, null);

    node.dispatchEvent(evt);
}

//an algorithm for pure bookkeeping

var func_a1 = function(url,xpath,url1,url2,targetInfoString,iMacrosTargetInfoString,similarityInfoString,ATAQVInfoString,targetUrl,iMacrosUrl,similarityUrl,ATAQVUrl){
	var output = url+"<,>"+xpath+"<,>"+url1+"<,>"+url2;
	var target = null;
	var iMacrosWorked = 0;
	var votingWorked = 0;
	var similarityWorked = 0;
	var ATAWorked = 0;
	var iMacrosMaybeWorked = 0;
	var votingMaybeWorked = 0;
	var similarityMaybeWorked = 0;
	var ATAMaybeWorked = 0;
	
	
	var targetInfo = JSON.parse(similarityInfoString);
	var simTarget = getTargetForSimilarity(targetInfo);
	
	if (url2 === similarityUrl){ //prefer to use similarity, so we can get best score and second best score
	    similarityWorked = 1;
		target = simTarget;
	}
	if (url1 !== similarityUrl){
	  similarityMaybeWorked = 1;
	}
	
	if (url2 === targetUrl){
	    votingWorked = 1;
	    if (target === null){
			var targetInfo = JSON.parse(targetInfoString);
			target = getTarget(targetInfo);
		}
	}
	if (url1 !== targetUrl){
	  votingMaybeWorked = 1;
	}
	
	if (url2 === iMacrosUrl){
		iMacrosWorked = 1;
	    if (target === null){
			var targetInfo = JSON.parse(iMacrosTargetInfoString);
			target = retrieveTargetForIMacros(targetInfo);
		}
	}
	if (url1 !== iMacrosUrl){
	  iMacrosMaybeWorked = 1;
	}
	
	
	if (url2 === ATAQVUrl){
		ATAWorked = 1;
	    if (target === null){
			var targetInfo = JSON.parse(ATAQVIngetTargetForATAQVfoString);
			target = getTargetForATAQV(ATAQVInfoString);
		}
	}
	if (url1 !== ATAQVUrl){
	  ATAMaybeWorked = 1;
	}

	
	output += "<,>"+iMacrosWorked+"<,>"+iMacrosMaybeWorked+"<,>"+votingWorked+"<,>"+votingMaybeWorked+"<,>"+similarityWorked+"<,>"+similarityMaybeWorked+"<,>"+ATAWorked+"<,>"+ATAMaybeWorked;
	output += "<,>"+bestScore+"<,>"+secondBestScore;
	
	if (target === null){
		return output; //couldn't find a correct node, don't emit feature info
	}
	var origTargetInfo = JSON.parse(similarityInfoString);
	var newTargetInfo = getFeatures(target);
	for (var key in origTargetInfo){
		output += "<,>"+key+"<,>"+origTargetInfo[key]+"<,>"+newTargetInfo[key];
	}
	return output;
};