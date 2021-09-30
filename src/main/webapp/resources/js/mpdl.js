

let removeButton= `
    <span class="input-group-btn">
        <button class="btn btn-default" type="button" onclick="removeAffiliationId(this); return false;">
            <span class="glyphicon glyphicon-remove no-text" aria-hidden="true"></span>
        </button>
    </span>`;

function removeAffiliationId(buttonElement){
    let affiliationIdFieldName = $(buttonElement).attr("data-for-affiliationId");
    let affiliationTextFieldName = $(buttonElement).attr("data-for-affiliationText");

    let affiliationIdField = $(buttonElement).closest(".form-group").find("input[data-fieldtype-name='" + affiliationIdFieldName + "']");
    let affiliationTextField = $(buttonElement).closest(".form-group").find("input[data-fieldtype-name='" + affiliationTextFieldName + "']");

    affiliationIdField.val("");
    affiliationTextField.val("");
    affiliationIdField.prop("readonly",false);
    affiliationTextField.prop("readonly",false);

    //Remove delete button
    $(buttonElement).remove();

    //Move input field back out of input group
    let inputGroup = affiliationTextField.parent();
    affiliationTextField.parent().parent().append(affiliationTextField);

    //Remove input group
    inputGroup.remove();

    bindAffiliationInputField(affiliationTextField, affiliationIdFieldName);




}
function addAffiliationId(affiliationTextField, affiliationIdField){
    //Set both fields readonly
    affiliationIdField.prop("readonly",true);
    affiliationTextField.prop("readonly",true);

    //Create and append an bootstrap input group for the delete button
    let inputGroup = $('<div class="input-group"/>');
    affiliationTextField.parent().append(inputGroup);

    //add the text field and the remove button to the input group
    inputGroup.prepend(affiliationTextField);
    let affRemoveButton = $(removeButton)
    affRemoveButton.find("button")
        .attr("data-for-affiliationText", affiliationTextField.attr("data-fieldtype-name"))
        .attr("data-for-affiliationId", affiliationIdField.attr("data-fieldtype-name"));

    inputGroup.append(affRemoveButton);
}
//function to bind an autosuggest for organization identifiers
function bindAffiliationAutoComplete() {


    $("input[data-fieldtype-name='authorAffiliation']").each(function () {
        bindAffiliationInputField(this, "authorAffiliationId");
    });
    $("input[data-fieldtype-name='authorAffiliation2']").each(function () {
        bindAffiliationInputField(this, "authorAffiliation2Id");
    });
    $("input[data-fieldtype-name='authorAffiliation3']").each(function () {
        bindAffiliationInputField(this, "authorAffiliation3Id");
    });
}

function bindAffiliationInputField(element, nameOfIdElement) {
    console.log("Rebind Autosuggest for " + element);
    let idField = $(element).parent().parent().find("input[data-fieldtype-name='" + nameOfIdElement + "']");

    if(idField) {
        if (idField.val()) {
            addAffiliationId($(element), idField);
        } else {
            let autocompleteWidget = $(element).autocomplete({
                source: function (request, response) {
                    $.ajax({
                        url: "/api/controlledvocabulary/ror",
                        dataType: "json",
                        data: {
                            q: request.term
                        },
                        success: function (data) {
                            response($.map(data.response.docs, function (item) {
                                return {
                                    label: item['name'][0],
                                    value: item['name'][0],
                                    id: item['id']
                                };
                            }));
                        }
                    });
                },
                select: function (event, ui) {
                    $(element).val(ui.item.label);
                    idField.val(ui.item.id);
                    addAffiliationId($(element), idField);


                },

                minLength: 2,


            }).autocomplete("widget");

            //wrap the suggest list into a span to enable CSS class scoping, otherwise jqueryui css conflicts with primefaces css
            autocompleteWidget.wrap("<span class='jqueryui'></span>");
        }
    }
}


function bindTaggleKeywordInput() {


    $("div[data-fieldtype-name='keyword']").each(function () {
        console.log("Found keyword div" + this);
        //let select = $(this).next("select");
        //console.log("Found  select" + select);
        //let divElement = this.get();
        let hiddenInput = $(this).next("input");
        let taggle = new Taggle(this, {
            placeholder: $(hiddenInput).attr('placeholder'),
            saveOnBlur: true,
            preserveCase: true,
            onTagAdd : function (event, tagText) {
                console.log("Tag Added");
                setTagValuesInInput(hiddenInput,taggle.getTagValues());
                /*
                select.attr("size", taggle.getTagValues().length);
                select.append("<option selected='selected' value='" + tagText + "'>" + tagText + "</option>")
                */

            },
            onTagRemove : function (event, tagText) {
                setTagValuesInInput(hiddenInput,taggle.getTagValues());
            }


        });
        var currentValues = $(hiddenInput).val().split('||');
        taggle.add(currentValues);
    });

    function setTagValuesInInput(textAreaElement, tagArray)
    {
        $(textAreaElement).val(tagArray.join('||'));
    }

    /*
    let divElement = $("div[data-fieldtype-name='keywordValue.parent']").get()[0];
    if(divElement) {
        let keywordElementName = $("input[data-fieldtype-name='keywordValue']").attr("name");
        $("input[data-fieldtype-name='keywordValue']").remove();
        //datasetForm:j_idt502:0:j_idt505:9:j_idt546:1:j_idt548:0:inputText
        //datasetForm:j_idt502:0:j_idt505:9:j_idt546:0:j_idt548:0:inputText

        const keyWordValueBaseIdParts = keywordElementName.split(":");
        console.log(keyWordValueBaseIdParts);
        console.log(divElement.getAttribute("class"));
        new Taggle(divElement, {
            hiddenInputNameFunction : function(index){
                keyWordValueBaseIdParts[6] = index;
                return keyWordValueBaseIdParts.join(":");
            }

        });
    }
    */



}

//Rebind autosuggest after ajax requests (+/- Buttons)
$(document).on('pfAjaxComplete', function() {
    bindAffiliationAutoComplete();
    bindTaggleKeywordInput();
});

