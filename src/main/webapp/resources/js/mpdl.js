

let removeButton= `
    <span class="input-group-btn">
        <button class="btn btn-default" type="button" onclick="removeAffiliationId(this); return false;">
            <span class="glyphicon glyphicon-remove no-text" aria-hidden="true"></span>
        </button>
    </span>`;
    
let orcidRemoveButton= `
    <span class="input-group-btn">
        <button class="btn btn-default" type="button" onclick="removeAuthorIdentifier(this); return false;">
            <span class="glyphicon glyphicon-remove no-text" aria-hidden="true"></span>
        </button>
    </span>`;

const ORCID_URI = 'https://orcid.org/';

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

function removeAuthorIdentifier(buttonElement){
    let authorIdentifierFieldName = $(buttonElement).attr("data-for-authorIdentifier");
    let authorNameFieldName = $(buttonElement).attr("data-for-authorName");

    let authorIdentifierField = $(buttonElement).closest(".form-group").find("input[data-fieldtype-name='" + authorIdentifierFieldName + "']");
    let authorNameField = $(buttonElement).closest(".form-group").find("input[data-fieldtype-name='" + authorNameFieldName + "']");

    authorIdentifierField.val("");
    authorNameField.val("");
    authorIdentifierField.prop("readonly",false);
    authorNameField.prop("readonly",false);

    //Remove delete button
    $(buttonElement).remove();

    //Move input field back out of input group
    let inputGroup = authorNameField.parent();
    authorNameField.parent().parent().append(authorNameField);

    //Remove input group
    inputGroup.remove();

    bindAuthorNameInputField(authorNameField, authorIdentifierFieldName);

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

function addAAuthorIdentifier(authorNameField, authorIdentifierField){
    //Set both fields readonly
    authorNameField.prop("readonly",true);
    authorIdentifierField.prop("readonly",true);

    //Create and append an bootstrap input group for the delete button
    let inputGroup = $('<div class="input-group"/>');
    authorNameField.parent().append(inputGroup);

    //add the text field and the remove button to the input group
    inputGroup.prepend(authorNameField);
    let authorRemoveButton = $(orcidRemoveButton)
    authorRemoveButton.find("button")
        .attr("data-for-authorName", authorNameField.attr("data-fieldtype-name"))
        .attr("data-for-authorIdentifier", authorIdentifierField.attr("data-fieldtype-name"));

    inputGroup.append(authorRemoveButton);
}

//function to bind an autosuggest for organization identifiers & ORCIDs
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
    $("input[data-fieldtype-name='authorName']").each(function () {
        bindAuthorNameInputField(this, "authorIdentifier");
    });
}


function bindAffiliationInputField(element, nameOfIdElement) {
    // console.log("Rebind Autosuggest for " + element);
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

function bindAuthorNameInputField(element, nameOfIdElement) {
    // console.log("Rebind Autosuggest for " + element);
    let idField = $(element).parent().parent().find("input[data-fieldtype-name='" + nameOfIdElement + "']");

    if(idField) {
        if (idField.val()) {
            addAAuthorIdentifier($(element), idField);
        } else {
            let orcidAutocompleteWidget = $(element).autocomplete({
                source: function (request, response) {
                    $.ajax({
                        url: "https://pub.orcid.org/v3.0/expanded-search",
                        dataType: "json",
                        data: {
                            q: request.term,
                            rows: 50
                        },
                        headers: {
                            'Accept': 'application/json'
                        },
                        success: function (data) {
                            response($.map(data['expanded-result'], function (item) {
                                return {
                                    value: item['family-names'] + ", " + item['given-names'],
                                    // label: item['given-names'] + " " + item['family-names'] + " " + ((item['institution-name'].length > 0) ? ", " + item['institution-name'][0] : ""),
                                    label: item['given-names'] + " " + item['family-names'] + " " + item['orcid-id'],
                                    id: item['orcid-id']
                                };
                            }));
                        }
                    });
                },
                select: function (event, ui) {
                    $(element).val(ui.item.value);
                    idField.val(ORCID_URI + ui.item.id);
                    addAAuthorIdentifier($(element), idField);


                },

                minLength: 3,


            }).autocomplete("widget");

            //wrap the suggest list into a span to enable CSS class scoping, otherwise jqueryui css conflicts with primefaces css
            orcidAutocompleteWidget.wrap("<span class='jqueryui'></span>");
        }
    }
}



function bindTaggleKeywordInput() {


    $("div[data-fieldtype-name='keyword']").each(function () {

        //if taggle is not activated yet
        if($(this).has('ul.taggle_list').length == 0)
        {
            let hiddenInput = $(this).next("input");

            let taggle = new Taggle(this, {
                placeholder: $(hiddenInput).attr('placeholder'),
                saveOnBlur: true,
                preserveCase: true,
                onTagAdd : function (event, tagText) {
                    //console.log("Tag Added");
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
            const currentValues = $(hiddenInput).val().split('||');
            taggle.add(currentValues);
        }

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

