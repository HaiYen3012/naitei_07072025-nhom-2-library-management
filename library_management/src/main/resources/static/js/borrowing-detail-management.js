// JavaScript for borrowing detail management
document.addEventListener('DOMContentLoaded', function() {
    
    // Handle update receipt button
    const updateBtn = document.getElementById('updateReceiptBtn');
    if (updateBtn) {
        updateBtn.addEventListener('click', function() {
            
            // Collect all dropdown changes
            const updates = [];
            const dropdowns = document.querySelectorAll('.status-dropdown');
            
            dropdowns.forEach(function(dropdown) {
                const detailId = dropdown.getAttribute('data-detail-id');
                const selectedValue = dropdown.value;
                
                if (selectedValue && selectedValue !== '') {
                    const row = dropdown.closest('tr');
                    const updateData = {
                        borrowingDetailId: parseInt(detailId),
                        action: selectedValue
                    };
                    
                    // Add additional data based on action
                    if (selectedValue === 'RETURN') {
                        const returnInput = row.querySelector('.return-date-input');
                        const returnDate = returnInput ? returnInput.value : new Date().toISOString().split('T')[0];
                        updateData.actualReturnDate = returnDate + 'T00:00:00';
                    }
                    
                    updates.push(updateData);
                }
            });
            
            if (updates.length === 0) {
                alert('Không có thay đổi nào để cập nhật!');
                return;
            }
            
            if (!confirm(`Bạn có chắc chắn muốn cập nhật ${updates.length} thay đổi?`)) {
                return;
            }
            
            // Show loading
            updateBtn.disabled = true;
            updateBtn.innerHTML = 'Đang cập nhật...';
            
            // Get receipt ID from URL
            const receiptId = window.location.pathname.split('/').pop();
            
            // Send request
            fetch(`/admin/borrow-requests/${receiptId}/update-details`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(updates)
            })
            .then(response => response.text())
            .then(data => {
                if (data === 'success') {
                    alert('Cập nhật thành công!');
                    window.location.reload();
                } else {
                    alert('Có lỗi xảy ra: ' + data);
                }
            })
            .catch(error => {
                alert('Có lỗi xảy ra khi cập nhật');
            })
            .finally(() => {
                updateBtn.disabled = false;
                updateBtn.innerHTML = '<i class="mdi mdi-content-save mr-2"></i><span>Cập nhật phiếu mượn</span><i class="mdi mdi-arrow-right ml-2"></i>';
            });
        });
    }
    
    // Handle extend buttons - simple alert for now
    document.addEventListener('click', function(event) {
        if (event.target.classList.contains('extend-btn') || event.target.closest('.extend-btn')) {
            event.preventDefault();
            alert('Chức năng gia hạn sẽ được triển khai sau');
        }
    });
    
    // Handle dropdown changes
    const dropdowns = document.querySelectorAll('.status-dropdown');
    dropdowns.forEach(function(dropdown) {
        dropdown.addEventListener('change', function() {
            const row = this.closest('tr');
            const selectedValue = this.value;
            
            // Hide all conditional inputs
            const returnDateGroup = row.querySelector('.return-date-group');
            
            if (returnDateGroup) returnDateGroup.style.display = 'none';
            
            // Show appropriate inputs
            if (selectedValue === 'RETURN') {
                if (returnDateGroup) {
                    returnDateGroup.style.display = 'block';
                    const returnInput = row.querySelector('.return-date-input');
                    if (returnInput) {
                        returnInput.value = new Date().toISOString().split('T')[0];
                    }
                }
            }
        });
    });
});
