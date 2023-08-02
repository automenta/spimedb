class Menu /* extends View */ {
    
    constructor(elementID) {
        this.ele = $("#" + elementID);
        //$.sidebarMenu(this.ele);
        this.activeIcon = null;
        this.active = null;
    }

    addMenu(id, content) {
        const i = $('<a>').addClass('label buttonlike').text(id);
        //const u = $('<div>').addClass('sidebar-submenu').append(content());
        //u.hide();

        i.click(()=>{
            if (this.active===id) {
                //same; toggle
                this.active = null;
                this.popup.remove();
            } else {
                if (this.active)
                    this.popup.remove();

                this.active = id;
                this.popup = $('<div>').addClass('modal cell');
                this.popup.append(content());
                $('body').append(this.popup);
            }

            // if (this.active) {
            //     this.active.remove();
            //     this.active = null;
            // }
            // if (this.activeIcon !== i) {
            //     this.ele.after(this.active = $('<div class="popup">').append(content()));
            //     if (this.activeIcon!=null)
            //         this.activeIcon.removeClass('menuActive');
            //     i.addClass('menuActive');
            //     this.activeIcon = i;
            // } else {
            //     i.removeClass('menuActive');
            //     this.activeIcon = null; //hide
            // }
        });

        this.ele.append($('<div>').addClass('cell').append(i));
    }
}