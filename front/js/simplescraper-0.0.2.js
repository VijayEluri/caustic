/**
   SimpleScraper Front 0.0.1

   Copyright 2010, AUTHORS.txt
   Licensed under the MIT license.

   Requires jQuery, tested with 1.4.4

**/

(function (jQuery) {
    // Prevent console failures.
    if(!window.console) {
	window.console = {};
	window.console.log = function(string) { return false; }
	console = {};
	console.log = function(string) { return false; }
    }

    /** simplescraper global settings. **/
    settings = {
	backDirectory: '/back'
    };

    classes = {
	closer: 'closer',
	selector: 'selector',
	updater: 'updater',
	deleter: 'deleter',
	attributer: 'attributer',
	tagHolder: 'tagHolder',
	tag: 'tag',
	tagger: 'tagger',
	untagger: 'untagger',
	editor: 'editor',
	model: 'model',
	resource: 'resource',
	resourceEditor:'resourceEditor',
	title: 'title',
	resourceControls: 'upperright'
    };
    text = {
	closer: 'close',
	updater: 'update',
	deleter: 'delete'
    };
    events = {
	'delete': 'delete.simplescraper',
	'put':    'put.simplescraper',
	'get':    'get.simplescraper'
    };

    functions = {
	// Give ajax calls a standard error functionality.
	ajax: function(options) {
	    $.extend(options,
		     {
			 error: function(response, code) {
			     $.error(response.status + ': ' + response.responseText);
			 }
		     });
	    $.ajax(options);
	}
    };

    /** simplescraper editor. **/
    /*(function ( $ ) {
	var widgets = {
	    selector: function(name, url, preSelection) {
		if(!name || !url)
		    $.error('Must specify name and url to generate a selector.');
		var $selector = $('<select>').addClass(classes.selector).addClass(name);
		functions.ajax({
		    type: 'get',
		    url: url,
		    dataType: 'json',
		    success: function(data) {
			for(var i = 0; i < data.length; i++) {
			    $option = $('<option>').append(data[i]);
			    if(preSelection == data[i])
				$option.attr('selected', true);
			    $selector.append($option);
			}
			//$selector.trigger('change');
		    }
		});
		return $selector;
	    }
	};
	var methods = {
	    init: function() {
		return this.each(function() {
		    var $editor = $(this).addClass(classes.editor);
		    var $selectModel = widgets['selector'](classes.model,settings.backDirectory + '/');
		    $selectModel.bind('change', function() {
			$editor.simplescraper_editor('refresh');
		    });
		    //$editor.append($('<p>Model:</p>').append($selectModel));
		    
		    $editor.append($('<p>').append(
			$('<input>').attr('type', 'text'))
			.append($('<span>Add</span>').click(function() {
			    $editor.simplescraper_editor('createResource')
			})));
		});
	    },
	    // Update the list of resources. Optional auto-select. 
	    refresh: function(preSelection) {
		return this.each(function() {
		    var $editor = $(this);
		    var $selectModel = $editor.find('.' + classes.model);
		    $editor.find('.' + classes.resourceEditor).remove();
		    $editor.append($('<p>').append(
			widgets['selector'](
			    classes.resourceEditor,
			    settings.backDirectory + '/' + $selectModel.val() + '/', preSelection)
			    .bind('change', function() {
				$editor.simplescraper_editor('viewResource')
			    })).append('<span>Edit</span>').addClass(classes.resourceEditor).click(function() {
				$editor.simplescraper_editor('viewResource')
			    }));
		});
	    }, 
           // Determine what current selections are.
	    selected: function() {
		var array = [];
		this.each(function() {
		    var $editor = $(this);
		    array.push({
			model: $editor.find('.' + classes.model).val(),
			resource: $editor.find('select.' + classes.resourceEditor).val(),
			input: $editor.find('input').val()
		    });
		});
		if(array.length == 1)
		    return array[0];
		return array;
	    },
	    createResource: function() {
		return this.each(function() {
		    var $editor = $(this);
		    var selections = $editor.simplescraper_editor('selected');
		    if(!settings.backDirectory || !selections.model || !selections.input)
			return;
		    functions.ajax({
			type: 'put',
			url: settings.backDirectory + '/' + selections.model + '/' + selections.input,
			success: function() {
			    $editor.simplescraper_editor('refresh', selections.input);
			    $editor.simplescraper_editor('viewResource');
			}
		    });
		});
	    },
	    viewResource: function() {
		return this.each(function() {
		    var selections = $(this).simplescraper_editor('selected');
		    $('body').append($('<div>').simplescraper_resource('init', selections.model, selections.resource));
		});
	    }
	};
	$.fn.simplescraper_editor = function(method) {
	    if ( methods[method] ) {
		return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
	    } else if ( typeof method === 'object' || ! method ) {
		return methods.init.apply( this, arguments );
	    } else {
		$.error( 'Method ' +  method + ' does not exist in simplescraper_editor.' );
	    }
	};
    }) (jQuery);
*/
    /** simplescraper resource. **/
    (function ( $ ) {
	/* Widget generation to create components of simplescraper. */
	var widgets = {
	    /* Triggers resource updates. */
	    updater: function() {
		return $('<span>').append(text.updater).addClass(classes.updater).click(function() {
		    $(this).closest('.' + classes.resource).trigger('update.simplescraper');
		});
	    },
	    /* Triggers resource deletion. */
	    deleter: function() {
		return $('<span>').append(text.deleter).addClass(classes.deleter).click(function() {
		    $(this).closest('.' + classes.resource).trigger('delete.simplescraper');
		});
	    },
	    /* Generate an attribute input. */
	    attributer: function(name, value) {
		return $('<span>').append(name + ': ').addClass(classes.attributer).append($('<textarea>').attr({name: name, value: value}));
	    },
	    /* A tag holder. */
	    tagHolder: function(name, ids) {
		var $tagHolder = $('<div>').append(name + ': ').addClass(classes.tagHolder);
		for(var i = 0; i < ids.length; i++) {
		    $tagHolder.append(widgets['tag'](name, ids[i]));
		}
		$tagHolder.append(widgets['tagger'](name));
		return $tagHolder;
	    },
	    /* A tagger. Allows the user to add tags. */
	    tagger: function(name) {
		return $('<input>').addClass(classes.tagger).data('name',name);
	    },
	    /* A tag. Opens itself as a resource when clicked.  */
	    tag: function(name, id) {
		return $('<span>').append(id).addClass(classes.tag).data({name: name, id: id})
		    .click(function() {
			// TODO  this should follow a redirect now.
			//$('body').append($('<div>').simplescraper('init', name.replace(/e?s$/, ''), id))
			$('body').append($('<div>').simplescraper('init', $(this).parent('.' + classes.resource).simplescraper('location') + '/' + name, id));
		    })
		    .append(widgets['untagger']);
	    },
	    /* Remove the tag this is attached to. */
	    untagger: function() {
		return $('<span>').append('X').addClass(classes.untagger);
	    },
	    /* Close this resource. */
	    closer: function() {
		return $('<span>').append('close').addClass(classes.closer).click(function() {
		    $(this).closest('.' + classes.resource).simplescraper('close')
		});
	    },
	    make: function(widgetType) {
		return $('<div />').addClass(classes[widgetType]);
	    }
	};
	var _widgets = {
	    'title' : {
		/* options.name */
		_init : function( options ) {
		    return $('<div />').text(options.name);
		},
		_class : '.title'
	    },
	    'resource' : {
		/* options.id, options.model */
		_init : function( options ) {
		    return $('<div />')
			.data({
			    simplescraper : { 
				model : options.model,
				id : options.id
			    }
			})
			.draggable()
			.append(_factory('title', { name: options.model + ' ' + options.id } ))
		        .append(_factory('close'))
			.append(_factory('delete'));
		},
		_class : '.resource',
		_bindings : {
		    /* Bring the resource up-to-date with the server. */
		    'get.simplescraper' : function( ) { 
			$(this).empty()

/////////////////////////
			/* Controls */
			    .append($('<div>').addClass(classes.resourceControls)
				    .append(widgets['deleter']).append(widgets['closer']));
			functions.ajax({
			    type: 'get',
			    url: $(this).data('location'),
			    dataType: 'json',
			    success: function(data)
			    {
				if('name' in data) { // If there's a name, place it first.
				    $resource.append($('<p>')).append(widgets['attributer']('name', data['name']));
				    delete data['name'];
				}
				for(var key in data) {
				    var attribute = data[key];
				    if(jQuery.isArray(data[key])) { // A collection of tags was retrieved.
					$resource.append($('<p>').append(widgets['tagHolder'](key, data[key])));
				    } else { // An individual, put-able value.
					$resource.append($('<p>').append(widgets['attributer'](key, data[key])));
				    }
				}
			    }
			});
		    },
		    'put.simplescraper' : function( ) { },
		    'delete.simplescraper' : function( ) { },
		    'close.simplescraper' : function( ) {
			$(this).remove();
		    },
		    'tag.simplescraper' : function( tag, name ) { },
		    'untag.simplescraper' : function( tag, name ) { }
		}
	    },
		    'close' : {
		_init : function ( ) { return $('<span />').text('X'); },
		_class : '.close',
		_bindings : {
		    'click' : function( ) { $(this).closest(_widgets._resource._class).trigger('close.simplescraper'); }
		}
	    },
            'delete' : {
		_init : function( ) { return $('<span />').text('delete'); },
		_class : '.delete',
		_bindings : {
		    'click' : function( ) { $(this).closest(_widgets._resource._class).trigger('delete.simplescraper'); }
		}
	    },
	    'tag' : {
		_init : function( options ) { return $('<span />').text(options.type); }
		_class : '.tag',
		_bindings : {
		    'delete.simplescraper' : function ( ) { }
		}
	    }
	};
	var _factory = function( type, options ) {
	    if( ! type in widgets )
		$.error(widget + ' is not a valid widget.');
	    var $widget = $(widgets.type._init(options));
	    if( _class in widgets.type )
		$widget.addClass(widgets.type._class.replace('/\./g', ' '));
	    return $widget;
	};
	var methods = {
	    init: function( options ){
		return this.each(function() {
		    var $this = $(this),
		    data = $this.data('simplescraper');
		    
		    if( ! data ) {
			$(this).data('simplescraper', {
			    resources : {}
			});
			
			/* Widget bindings. */
			for( type in _widgets ) {
			    var widget = _widgets.type;
			    if(widget._bindings && widget._class) {
				for( event in widget._bindings ) {
				    $this.find(widget._class).live(widget._bindings[event]);
				}
			    }
			}
		    }
		}).trigger(events.get);
	    },
	    destroy : function( ) {
		return this.each(function(){
		    var $this = $(this),
		    data = $this.data('simplescraper');
		    
		    /* Widget unbinding. */
		    for( type in _widgets ) {
			$this.find(widget.type._class).die('.simplescraper').remove();
		    }
		    data.simplescraper.remove();
		    $this.removeData('simplescraper');
		})
	    }

		/*return this.each(function() {
		    if(!creator || !id || !model)
			$.error('Must specify creator, model and id to create a resource.');
		    var $resource = $(this).addClass(classes.resource).data({model: model, id: id});
		    if($resource.draggable)
			$resource.draggable();
		    // Don't allow the same resource to appear in multiple windows.
		    var alreadyOpen = false;
		    // Check to see if one of these is already open.
		    $.each($('.' + classes.resource), function() {
			if($(this).simplescraper('identify') == $resource.simplescraper('identify'))
			    alreadyOpen = true;
		    });
		    if(alreadyOpen == true) {
			$resource.remove();
			return;
		    }
		    $resource.delegate('.' + classes.attributer, 'blur', function() {
			$resource.simplescraper('put');
			return false;
		    });
		    $resource.delegate('.' + classes.deleter, 'click', function() {
			$resource.simplescraper('delete');
			return false;
		    });
		    $resource.delegate('.' + classes.tagger, 'blur', function(){
			var $tagger = $(this);
			$resource.simplescraper('tag', $tagger.data('name'), $tagger.val());
			return false;
		    });
		    $resource.delegate('.' + classes.untagger, 'click', function() {
			var $tag = $(this).closest('.' + classes.tag);
			$resource.simplescraper('untag', $tag.data('name'), $tag.data('id'));
			return false;
		    });
		    $resource.simplescraper('get');
		});*/
	    /* Obtain a resource's attributes from the page. */
	    attributes: function() {
		var attributesAry = [];
		this.each(function() {
		    var $resource = $(this);
		    if(!$resource.hasClass(classes.resource)) // No attributes unless resource.
			return;
		    var attributes = {};
		    $.each($resource.find('.' + classes.attributer), function() {
			var $attributer = $(this);
			var $input =$attributer.find('textarea');
			attributes[$input.attr('name')] = $input.val();
		    });
		    attributesAry.push(attributes);
		});
		if(attributesAry.length == 1)
		    return attributesAry[0];
		return attributesAry;
	    },
	    /* Attempt to bring the server-side resource up-to-date with the page. */
	    put: function() {
		return this.each(function() {
		    var $resource = $(this);
		    if(!$resource.hasClass(classes.resource)) // Only resources can be updated.
			return;
		    var data = $resource.simplescraper('attributes');
		    var url = $resource.simplescraper('location');
		    if(!url)
			return;
		    $resource.empty();
		    functions.ajax({
			type: 'put',
			url: url,
			data: data,
			success: function(response) // TODO: check status
			{
			    if(data['id']) // keep ID up to date
				$resource.data('id', data['id']);
			    //$resource.simplescraper('get');
			    $('.' + classes.editor).simplescraper_editor('refresh');
			    $('.' + classes.resource).simplescraper('get'); // Could modify taggings in other displayed items.
			}
		    });
		});
	    },
	    /* Close a resource. */
	    close: function() {
		return this.each(function() {
		    $(this).remove();
		});
	    },
	    /* Delete a resource. */
	    delete: function() {
		return this.each(function() {
		    var $resource = $(this);
		    if(!$resource.hasClass(classes.resource)) // Only resources can be deleted.
			return;
		    var url = $resource.simplescraper('location');
		    if(!url)
			return;
		    functions.ajax({
			type: 'delete',
			url: url,
			success: function(contents)
			{// TODO: check status
			    $('.' + classes.editor).simplescraper_editor('refresh');
			    $resource.simplescraper('close');
			}
		    });
		})
	    },
	    /* Add a tag a resource. Must supply a type of tag and tag id. */
	    tag: function(tagType, tagId) {
		return this.each(function() {
		    var $resource = $(this);
		    if(!$resource.hasClass(classes.resource)) // Only resources can be tagged.
			return;
		    var url = $resource.simplescraper('location');
		    if(!url)
			return;
		    url = url + '/' + tagType + '/' + tagId
		    functions.ajax({
			type: 'put',
			url: url, // Pluralizes.
			success: function(contents)
			{
			    $resource.simplescraper('put'); // This will PUT possibly unsaved changes, which will also GET.
			}
		    });
		});
	    },
	    /* Untag a tag from a resource.  Must supply a type of tag and tag id. */
	    untag: function(tagType, tagId) {
		return this.each(function() {
		    var $resource = $(this);
		    if(!$resource.hasClass(classes.resource)) // Only resources can be untagged.
			return;
		    var url = $resource.simplescraper('location');
		    if(!url)
			return;
		    url = url + '/' + tagType + '/' + tagId, // Pluralizes.

		    functions.ajax({
			type: 'delete',
			url: url,
			success: function(contents)
			{
			    $resource.simplescraper('get');
			}
		    });
		});
	    }
	};

	$.fn.simplescraper = function(method) {
	    if ( methods[method] ) {
		return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
	    } else if ( typeof method === 'object' || ! method ) {
		return methods.init.apply( this, arguments );
	    } else {
		$.error( 'Method ' +  method + ' does not exist in simplescraper.' );
	    }
	};
    }) (jQuery);
}) (jQuery);