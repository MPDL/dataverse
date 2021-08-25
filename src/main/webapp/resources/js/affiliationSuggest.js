

let removeButton= `
    <span class="input-group-btn">
        <button class="btn btn-default" type="button" onclick="removeAffiliationId(this); return false;">
            <span class="glyphicon glyphicon-remove no-text" aria-hidden="true"></span>
        </button>
    </span>`;

function removeAffiliationId(buttonElement){
    let affiliationIdField = $(buttonElement).closest(".form-group").find("input[data-fieldtype-name='authorAffiliationId']");
    let affiliationTextField = $(buttonElement).closest(".form-group").find("input[data-fieldtype-name='authorAffiliation']");

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

    bindAffiliationInputField(affiliationTextField);




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
    inputGroup.append($(removeButton));
}
//function to bind an autosuggest for organization identifiers
function bindAffiliationAutoComplete() {


    $("input[data-fieldtype-name='authorAffiliation']").each(function () {
        bindAffiliationInputField(this);

    });
}

function bindAffiliationInputField(element) {
    console.log("Rebind Autosuggest for " + element);
    let idField = $(element).parent().parent().find("input[data-fieldtype-name='authorAffiliationId']");

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
                                    label: item['name'][0] + " (" + item['id'] + ")",
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

                minLength: 2

            }).autocomplete("widget");

            //wrap the suggest list into a span to enable CSS class scoping, otherwise jqueryui css conflicts with primefaces css
            autocompleteWidget.wrap("<span class='jqueryui'></span>");
        }
    }
}

//Rebind autosuggest after ajax requests (+/- Buttons)
$(document).on('pfAjaxComplete', function() {
    bindAffiliationAutoComplete();
});

