if(!!window.React ||
   !!document.querySelector('[data-reactroot], [data-reactid]'))
  console.log('React.js');

if(!!window.angular ||
   !!document.querySelector('.ng-binding, [ng-app], [data-ng-app], [ng-controller], [data-ng-controller], [ng-repeat], [data-ng-repeat]') ||
   !!document.querySelector('script[src*="angular.js"], script[src*="angular.min.js"]'))
  console.log('Angular.js');

if(!!window.Backbone) {console.log('Backbone.js') };
if(!!window.Ember) console.log('Ember.js');
if(!!window.Vue) console.log('Vue.js');
if(!!window.Meteor) console.log('Meteor.js');
if(!!window.Zepto) console.log('Zepto.js');
if(!!window.jQuery) console.log('jQuery.js');

var getRandDelay = function(){
 return Math.floor(500 + (Math.random() * 1000) );
}

function showAndroidToast(toast) { Android.showToast(toast); };



var uploadParsedResults = function(data){
    let st = JSON.stringify(data);
    Android.uploadParsedResults(st);
}

var triggerReactEvent = function(input, value){
    let lastValue = input.value;
    input.value = value;
    let event = new Event('input', {'text': value });
    // hack React15
    event.simulated = true;
    // hack React16
    let tracker = input._valueTracker;
    if (tracker) {
      tracker.setValue(lastValue);
    }
    input.dispatchEvent(event);
}

var getElementsByXPath = function(xpath, startNode, resultType) {
    startNode  = (typeof startNode === 'undefined') ? document : startNode;
    resultType = (typeof resultType === 'undefined') ? XPathResult.ANY_TYPE : resultType;

    return document.evaluate(xpath, startNode, null, resultType, null);
}

var getElementByXPath = function(xpath, startNode) {
    return getElementsByXPath(xpath, startNode, XPathResult.FIRST_ORDERED_NODE_TYPE).singleNodeValue;
}


//skyscanner functions
//day-list-total

var executeCheckFoundResults = function() {
    var timer = setInterval(function() {
        let d = checkFoundResults();
        showAndroidToast("day-list-total:" + d );

        if (d != "") {
            clearInterval(timer);
            let data = getPricesFromSkyScrapper();
            uploadParsedResults(data);
        }
      }, 2000);
}

var checkFoundResults = function() {
    let element = getElementByXPath("//div[contains(@class,'day-list-total')]");

    let d = "";
    if (element != null) {
        let patt = /(\d+) results/g;
        let parsing = patt.exec(element.textContent);
        if (parsing != null) {
            d = parsing[1];
        }
    }
    return d;
}

function searchFormSubmission(){

    showAndroidToast("search form start");

    //origin input
    triggerReactEvent(document.getElementById('js-origin-input'), 'ATL');

    showAndroidToast("origin airport selected");


    setTimeout(function() {

        //select first element in origin dropdown
        getElementByXPath('(//div[contains(@class,"-dataset-origin")]//*[contains(@class,"airport")])[1]').click();
        showAndroidToast("origin airport selected in dropdown");

        setTimeout(function() {

            //destination
            triggerReactEvent(document.getElementById('js-destination-input'), 'MSK');
            showAndroidToast("typed MSK");
            setTimeout(function() {
                //click on first element in destination dropdown
                getElementByXPath('(//div[contains(@class,"-dataset-destination")]//*[contains(@class,"airport")])[1]').click();
                showAndroidToast("click on first destination in the list");
                setTimeout(function() {
                    //click on submit button
                    getElementByXPath('//button[contains(@class,"fss-bpk-button--large")]').click();
                    showAndroidToast("form submitted");
                }, getRandDelay());
            }, getRandDelay());
        }, getRandDelay());

    }, getRandDelay());



    return document.title;
}

var getPricesFromSkyScrapper = function(){
    let flights = [];
    let i = 0;

    //let's store Ip address info as 0 object
    flights[i++] = ipData;


    let elements = getElementsByXPath('//li[contains(@class,"day-list-item")]');

    let currentElement = elements.iterateNext();

    let alertText = "Found Prices:\n";
    while (currentElement) {
      console.log(currentElement);
      let airlinesElement = getElementByXPath('.//span[contains(@class, "airline-text") or contains(@class,"airline-name")]/text()', currentElement);
      let priceElement = getElementByXPath('.//a[contains(@class, "price")]/text()', currentElement);

        let airline = (airlinesElement != null) ? airlinesElement.textContent : "";
        let priceC = (priceElement != null) ? priceElement.textContent: "";

        alertText += airline + " - " + priceC + "\n";

        flights[i++] = { line : airline, price: priceC};
        currentElement = elements.iterateNext();
    }
    return flights;
}


function getOffset( el ) {
    var _x = 0;
    var _y = 0;
    while( el && !isNaN( el.offsetLeft ) && !isNaN( el.offsetTop ) ) {
        _x += el.offsetLeft - el.scrollLeft;
        _y += el.offsetTop - el.scrollTop;
        el = el.offsetParent;
    }
    return { top: _y, left: _x };
}


function tapOnElement( el ) {
    var type = 'move'; // or start, end
    var event = document.createEvent('Event');
    event.initEvent('touch' + type, true, true);
    event.constructor.name; // Event (not TouchEvent)

    var offsetElement = getOffset(el);
    var x=offsetElement.top, y=offsetElement.left;

    var point = {x: x, y: y };
    event.touches = [{
        identifier: Date.now() + i,
        pageX: x,
        pageY: y,
        screenX: x,
        screenY: y,
        clientX: x,
        clientY: y
    }, {  identifier: Date.now() + i,
        pageX: point.x,
        pageY: point.y,
        screenX: point.x,
        screenY: point.y,
        clientX: point.x,
        clientY: point.y}]

        dispatchEvent(event);
}

var ipData = null;

function updateIpData(data)
{
    // save ip data on load
    ipData = data;
}

var s1 = document.createElement('script');
s1.src = '//freegeoip.net/json/?callback=updateIpData'

document.getElementsByTagName('head')[0].appendChild(s1);