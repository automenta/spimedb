/** from: https://github.com/huang-x-h/sidebar-menu/blob/master/dist/sidebar-menu.js
 *
 *  TODO dont add event handlers to elements that already have been initialized, allowing sidebarMenu to safely be called repeatedly
 *  TODO add edge resize drag handler
 * */
$.sidebarMenu = function(menu) {
    const animationSpeed = 100,
        subMenuSelector = '.sidebar-submenu';
    //
    // //prevent duplicate event handlers if called twice?
    // if (menu.data('sidebarMenuing')) {
    //     console.log('dupe');
    //     return;
    // }
    // menu.data('sidebarMenuing', 'x');

    $(menu).on('click', 'li a', function(e) {
        const $this = $(this);
        const ele = $this.next();

        const visible = ele.is(':visible');

        if (visible && ele.is(subMenuSelector)) {
            ele.slideUp(animationSpeed, function() {
                ele.removeClass('menu-open');
            });
            ele.parent("li").removeClass("active");
        } else if (!visible && ele.is(subMenuSelector)) {
            //Get the parent menu
            const parent = $this.parents('ul').first();
            //Close all open menus within the parent
            const ul = parent.find('ul:visible').slideUp(animationSpeed);
            //Remove the menu-open class from the parent
            ul.removeClass('menu-open');


            //Open the target menu and add the menu-open class
            ele.slideDown(animationSpeed, function() {
                //Add the class active to the parent li
                ele.addClass('menu-open');
                parent.find('li.active').removeClass('active');
                const parent_li = $this.parent("li"); //TODO maybe move this into the checkElement handler
                parent_li.addClass('active');
            });
        }

        //if this isn't a link, prevent the page from being redirected
        //if (hasSubMenu)
            //e.preventDefault();
    });
}
