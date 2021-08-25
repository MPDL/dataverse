

let removeButton= '<input type="button" value="X" onclick="removeAffiliationId(this); return false;"/>';

function removeAffiliationId(buttonElement){
    let affiliationIdField = $(buttonElement).parent().parent().find("input[data-fieldtype-name='authorAffiliationId']");
    let affiliationTextField = $(buttonElement).parent().parent().find("input[data-fieldtype-name='authorAffiliation']");
    affiliationIdField.val("");
    affiliationTextField.val("");


    affiliationIdField.prop("readonly",false);
    affiliationTextField.prop("readonly",false);

    $(buttonElement).remove();



}
function addAffiliationId(affiliationTextField, affiliationIdField){
    affiliationIdField.prop("readonly",true);
    affiliationTextField.prop("readonly",true);
    affiliationTextField.parent().append($(removeButton));
}
//function to bind an autosuggest for organization identifiers
function bindAffiliationAutoComplete() {


    $("input[data-fieldtype-name='authorAffiliation']").each(function () {


        let idField = $(this).parent().parent().find("input[data-fieldtype-name='authorAffiliationId']");

        if (idField.val()) {
            addAffiliationId($(this), idField);
        }

        else {
            let autocompleteWidget = $(this).autocomplete({
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
                    $(this).val(ui.item.label);
                    idField.val(ui.item.id);
                    addAffiliationId($(this), idField);


                },

                minLength: 2

            }).autocomplete("widget");

            //wrap the suggest list into a span to enable CSS class scoping, otherwise jqueryui css conflicts with primefaces css
            autocompleteWidget.wrap("<span class='jqueryui'></span>");
        }
    });
}

//Rebind autosuggest after ajax requests (+/- Buttons)
$(document).on('pfAjaxComplete', function() {
    bindAffiliationAutoComplete();
});

